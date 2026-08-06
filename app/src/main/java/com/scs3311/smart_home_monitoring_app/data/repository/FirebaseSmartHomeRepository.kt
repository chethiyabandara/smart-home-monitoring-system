package com.scs3311.smart_home_monitoring_app.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.scs3311.smart_home_monitoring_app.data.model.DeviceStatus
import com.scs3311.smart_home_monitoring_app.data.model.DeviceType
import com.scs3311.smart_home_monitoring_app.data.model.Floor
import com.scs3311.smart_home_monitoring_app.data.model.GridPosition
import com.scs3311.smart_home_monitoring_app.data.model.HomeState
import com.scs3311.smart_home_monitoring_app.data.model.LightSchedule
import com.scs3311.smart_home_monitoring_app.data.model.Room
import com.scs3311.smart_home_monitoring_app.data.model.SafetyAlert
import com.scs3311.smart_home_monitoring_app.data.model.SmartDevice
import com.scs3311.smart_home_monitoring_app.data.model.SwitchState
import com.scs3311.smart_home_monitoring_app.data.model.UsageLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

/**
 * Synchronizes the shared smart-home state with Realtime Database in real time.
 */
class FirebaseSmartHomeRepository : SmartHomeRepository {

    private val homeReference = FirebaseDatabase.getInstance().getReference("homes/home1")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _homeState = MutableStateFlow(emptyHomeState())
    override val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    private val _alerts = MutableStateFlow<List<SafetyAlert>>(emptyList())
    override val alerts: StateFlow<List<SafetyAlert>> = _alerts.asStateFlow()

    init {
        homeReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.toHomeState()
                _homeState.value = state
                _alerts.value = state.alerts
                Log.d(TAG, "Loaded ${state.devices.size} device(s) from Firebase")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Unable to read smart-home data: ${error.message}")
            }
        })
        scope.launch { runSafetyMonitor() }
        scope.launch { runScheduleMonitor() }
    }

    override suspend fun toggleDevice(deviceId: String) {
        val device = getDevice(deviceId) ?: return
        when (device.status) {
            DeviceStatus.OFF -> turnDeviceOn(device)
            DeviceStatus.ON -> turnDeviceOff(device)
            else -> Log.w(TAG, "Toggle ignored for $deviceId because it is ${device.status}")
        }
    }

    override suspend fun toggleSwitch(deviceId: String, switchId: String) {
        val device = getDevice(deviceId) ?: return
        if (device.type != DeviceType.MULTI_SWITCH) return
        val selectedSwitch = device.switches.find { it.id == switchId } ?: return
        if (!selectedSwitch.status.isControllable) return

        val nextSwitchStatus = if (selectedSwitch.status == DeviceStatus.ON) DeviceStatus.OFF else DeviceStatus.ON
        val switches = device.switches.map { switch ->
            if (switch.id == switchId) switch.copy(status = nextSwitchStatus) else switch
        }
        val nextPanelStatus = if (switches.any { it.status == DeviceStatus.ON }) DeviceStatus.ON else DeviceStatus.OFF
        val updates = mutableMapOf<String, Any?>(
            "devices/$deviceId/switches/$switchId/status" to nextSwitchStatus.name,
            "devices/$deviceId/status" to nextPanelStatus.name
        )

        if (device.status == DeviceStatus.OFF && nextPanelStatus == DeviceStatus.ON) {
            updates["devices/$deviceId/turnedOnAtMillis"] = ServerValue.TIMESTAMP
        } else if (device.status == DeviceStatus.ON && nextPanelStatus == DeviceStatus.OFF) {
            updates["devices/$deviceId/turnedOnAtMillis"] = null
            addUsageLogUpdate(updates, device, System.currentTimeMillis())
        }
        updateHome(updates, "toggle switch $switchId on $deviceId")
    }

    override suspend fun updateMaxOnDuration(deviceId: String, minutes: Int) {
        homeReference.child("devices").child(deviceId).child("maxOnDurationMinutes")
            .setValue(minutes.coerceIn(1, 120))
    }

    override suspend fun updateLightSchedule(deviceId: String, schedule: LightSchedule) {
        homeReference.child("devices").child(deviceId).child("lightSchedule").setValue(
            mapOf(
                "turnOnHour" to schedule.turnOnHour,
                "turnOnMinute" to schedule.turnOnMinute,
                "turnOffHour" to schedule.turnOffHour,
                "turnOffMinute" to schedule.turnOffMinute,
                "enabled" to schedule.enabled
            )
        )
    }

    override suspend fun refreshCameraSnapshot(deviceId: String) {
        homeReference.child("devices").child(deviceId).child("cameraSnapshotUrl")
            .setValue("https://picsum.photos/seed/${deviceId}-${System.currentTimeMillis()}/800/600")
    }

    override fun getDevice(deviceId: String): SmartDevice? =
        _homeState.value.devices.find { it.id == deviceId }

    override fun getUsageForDevice(deviceId: String): List<UsageLog> =
        _homeState.value.usageLogs.filter { it.deviceId == deviceId }

    override fun dismissAlert(alertId: String) {
        homeReference.child("alerts").child(alertId).child("acknowledged").setValue(true)
    }

    private fun turnDeviceOn(device: SmartDevice) {
        updateHome(
            mapOf(
                "devices/${device.id}/status" to DeviceStatus.ON.name,
                "devices/${device.id}/turnedOnAtMillis" to ServerValue.TIMESTAMP
            ),
            "turn on ${device.id}"
        )
    }

    private fun turnDeviceOff(device: SmartDevice, alertMessage: String? = null) {
        val updates = mutableMapOf<String, Any?>(
            "devices/${device.id}/status" to DeviceStatus.OFF.name,
            "devices/${device.id}/turnedOnAtMillis" to null
        )
        val now = System.currentTimeMillis()
        addUsageLogUpdate(updates, device, now)
        if (alertMessage != null) {
            val alertId = UUID.randomUUID().toString()
            updates["alerts/$alertId"] = mapOf(
                "deviceId" to device.id,
                "deviceName" to device.name,
                "message" to alertMessage,
                "timestampMillis" to now,
                "acknowledged" to false
            )
        }
        updateHome(updates, "turn off ${device.id}")
    }

    private fun addUsageLogUpdate(
        updates: MutableMap<String, Any?>,
        device: SmartDevice,
        endTimeMillis: Long
    ) {
        val startTimeMillis = device.turnedOnAtMillis ?: return
        val logId = UUID.randomUUID().toString()
        val durationMinutes = ((endTimeMillis - startTimeMillis) / 60_000L).toInt().coerceAtLeast(1)
        updates["logs/$logId"] = mapOf(
            "deviceId" to device.id,
            "deviceName" to device.name,
            "startTimeMillis" to startTimeMillis,
            "endTimeMillis" to endTimeMillis,
            "durationMinutes" to durationMinutes
        )
    }

    private fun updateHome(updates: Map<String, Any?>, action: String) {
        homeReference.updateChildren(updates) { error, _ ->
            if (error == null) Log.d(TAG, "Completed: $action")
            else Log.e(TAG, "Unable to $action: ${error.message}")
        }
    }

    private suspend fun runSafetyMonitor() {
        while (scope.isActive) {
            delay(SAFETY_CHECK_INTERVAL_MILLIS)
            val now = System.currentTimeMillis()
            _homeState.value.devices
                .filter { device ->
                    device.isSafetyCritical && device.status == DeviceStatus.ON &&
                        device.turnedOnAtMillis != null && device.maxOnDurationMinutes != null
                }
                .filter { device -> now - device.turnedOnAtMillis!! >= device.maxOnDurationMinutes!! * 60_000L }
                .forEach { device ->
                    turnDeviceOff(
                        device,
                        "${device.name} was automatically switched OFF after ${device.maxOnDurationMinutes} minutes."
                    )
                }
        }
    }

    private suspend fun runScheduleMonitor() {
        while (scope.isActive) {
            delay(SCHEDULE_CHECK_INTERVAL_MILLIS)
            val calendar = Calendar.getInstance()
            val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            _homeState.value.devices
                .filter { it.type == DeviceType.LIGHT && it.lightSchedule?.enabled == true }
                .forEach { device ->
                    val schedule = device.lightSchedule ?: return@forEach
                    val shouldBeOn = isWithinSchedule(nowMinutes, schedule)
                    if (shouldBeOn && device.status == DeviceStatus.OFF) turnDeviceOn(device)
                    if (!shouldBeOn && device.status == DeviceStatus.ON) turnDeviceOff(device)
                }
        }
    }

    private fun isWithinSchedule(nowMinutes: Int, schedule: LightSchedule): Boolean {
        val onMinutes = schedule.turnOnHour * 60 + schedule.turnOnMinute
        val offMinutes = schedule.turnOffHour * 60 + schedule.turnOffMinute
        return if (onMinutes <= offMinutes) nowMinutes in onMinutes until offMinutes
        else nowMinutes >= onMinutes || nowMinutes < offMinutes
    }

    private fun DataSnapshot.toHomeState(): HomeState {
        if (!exists()) return emptyHomeState()

        val rooms = child("rooms").children.map { room ->
            Room(
                id = room.key.orEmpty(),
                name = room.string("name"),
                gridRow = room.int("gridRow"),
                gridCol = room.int("gridCol"),
                rowSpan = room.int("rowSpan", 1),
                colSpan = room.int("colSpan", 1)
            )
        }
        val floors = child("floors").children.map { floor ->
            Floor(
                id = floor.key.orEmpty(),
                name = floor.string("name"),
                rooms = rooms.filter { room ->
                    child("rooms").child(room.id).string("floorId") == floor.key
                }
            )
        }

        return HomeState(
            homeName = string("name", "My Smart Home"),
            floors = floors,
            devices = child("devices").children.map { it.toSmartDevice() },
            usageLogs = child("logs").children.map { it.toUsageLog() },
            alerts = child("alerts").children
                .filterNot { it.boolean("acknowledged") }
                .map { it.toSafetyAlert() }
        )
    }

    private fun DataSnapshot.toSmartDevice() = SmartDevice(
        id = key.orEmpty(),
        name = string("name"),
        type = enumValue(string("type"), DeviceType.OUTLET),
        roomId = string("roomId"),
        floorId = string("floorId"),
        status = enumValue(string("status", DeviceStatus.OFF.name), DeviceStatus.OFF),
        gridPosition = GridPosition(int("gridRow"), int("gridCol")),
        switches = child("switches").children.map { switch ->
            SwitchState(
                id = switch.key.orEmpty(),
                name = switch.string("name"),
                status = enumValue(switch.string("status", DeviceStatus.OFF.name), DeviceStatus.OFF)
            )
        },
        maxOnDurationMinutes = child("maxOnDurationMinutes").value?.toString()?.toIntOrNull(),
        turnedOnAtMillis = child("turnedOnAtMillis").value?.toString()?.toLongOrNull(),
        lightSchedule = child("lightSchedule").takeIf { it.exists() }?.let { schedule ->
            LightSchedule(
                turnOnHour = schedule.int("turnOnHour"),
                turnOnMinute = schedule.int("turnOnMinute"),
                turnOffHour = schedule.int("turnOffHour"),
                turnOffMinute = schedule.int("turnOffMinute"),
                enabled = schedule.boolean("enabled", true)
            )
        },
        cameraSnapshotUrl = child("cameraSnapshotUrl").getValue(String::class.java),
        isSafetyCritical = boolean("isSafetyCritical")
    )

    private fun DataSnapshot.toUsageLog() = UsageLog(
        id = key.orEmpty(),
        deviceId = string("deviceId"),
        deviceName = string("deviceName"),
        startTimeMillis = long("startTimeMillis"),
        endTimeMillis = long("endTimeMillis"),
        durationMinutes = int("durationMinutes")
    )

    private fun DataSnapshot.toSafetyAlert() = SafetyAlert(
        id = key.orEmpty(),
        deviceId = string("deviceId"),
        deviceName = string("deviceName"),
        message = string("message"),
        timestampMillis = long("timestampMillis")
    )

    private fun DataSnapshot.string(name: String, default: String = ""): String =
        child(name).getValue(String::class.java) ?: default

    private fun DataSnapshot.int(name: String, default: Int = 0): Int =
        child(name).value?.toString()?.toIntOrNull() ?: default

    private fun DataSnapshot.long(name: String): Long =
        child(name).value?.toString()?.toLongOrNull() ?: 0L

    private fun DataSnapshot.boolean(name: String, default: Boolean = false): Boolean =
        child(name).getValue(Boolean::class.java) ?: default

    private inline fun <reified T : Enum<T>> enumValue(value: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private fun emptyHomeState() = HomeState(
        homeName = "My Smart Home",
        floors = emptyList(),
        devices = emptyList(),
        usageLogs = emptyList(),
        alerts = emptyList()
    )

    private companion object {
        const val TAG = "FirebaseHome"
        const val SAFETY_CHECK_INTERVAL_MILLIS = 10_000L
        const val SCHEDULE_CHECK_INTERVAL_MILLIS = 60_000L
    }
}
