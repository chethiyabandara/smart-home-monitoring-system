package com.scs3311.smart_home_monitoring_app.data.repository

import com.scs3311.smart_home_monitoring_app.data.DemoData
import com.scs3311.smart_home_monitoring_app.data.model.DeviceStatus
import com.scs3311.smart_home_monitoring_app.data.model.DeviceType
import com.scs3311.smart_home_monitoring_app.data.model.HomeState
import com.scs3311.smart_home_monitoring_app.data.model.LightSchedule
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
import java.util.UUID

/**
 * In-memory repository simulating Firebase Realtime Database sync.
 * Safety cutoff worker runs every 10 seconds to auto-turn-off safety-critical devices.
 */
class DemoSmartHomeRepository : SmartHomeRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _homeState = MutableStateFlow(DemoData.initialHome)
    override val homeState: StateFlow<HomeState> = _homeState.asStateFlow()

    private val _alerts = MutableStateFlow<List<SafetyAlert>>(emptyList())
    override val alerts: StateFlow<List<SafetyAlert>> = _alerts.asStateFlow()

    init {
        scope.launch { runSafetyMonitor() }
        scope.launch { runLightScheduleWorker() }
    }

    override suspend fun toggleDevice(deviceId: String) {
        updateDevice(deviceId) { device ->
            if (!device.status.isControllable) return@updateDevice device
            val newStatus = if (device.status == DeviceStatus.ON) DeviceStatus.OFF else DeviceStatus.ON
            val now = System.currentTimeMillis()
            if (newStatus == DeviceStatus.ON) {
                device.copy(status = newStatus, turnedOnAtMillis = now)
            } else {
                logUsage(device, device.turnedOnAtMillis, now)
                device.copy(status = newStatus, turnedOnAtMillis = null)
            }
        }
    }

    override suspend fun toggleSwitch(deviceId: String, switchId: String) {
        updateDevice(deviceId) { device ->
            if (device.type != DeviceType.MULTI_SWITCH) return@updateDevice device
            val updatedSwitches = device.switches.map { sw ->
                if (sw.id == switchId && sw.status.isControllable) {
                    val newStatus = if (sw.status == DeviceStatus.ON) DeviceStatus.OFF else DeviceStatus.ON
                    sw.copy(status = newStatus)
                } else sw
            }
            val anyOn = updatedSwitches.any { it.status == DeviceStatus.ON }
            device.copy(
                switches = updatedSwitches,
                status = if (anyOn) DeviceStatus.ON else DeviceStatus.OFF
            )
        }
    }

    override suspend fun updateMaxOnDuration(deviceId: String, minutes: Int) {
        updateDevice(deviceId) { it.copy(maxOnDurationMinutes = minutes.coerceIn(1, 120)) }
    }

    override suspend fun updateLightSchedule(deviceId: String, schedule: LightSchedule) {
        updateDevice(deviceId) { it.copy(lightSchedule = schedule) }
    }

    override suspend fun refreshCameraSnapshot(deviceId: String) {
        updateDevice(deviceId) { device ->
            if (device.type != DeviceType.CAMERA) return@updateDevice device
            val seed = System.currentTimeMillis()
            device.copy(
                cameraSnapshotUrl = "https://picsum.photos/seed/$seed/800/600"
            )
        }
    }

    override fun getDevice(deviceId: String): SmartDevice? =
        _homeState.value.devices.find { it.id == deviceId }

    override fun getUsageForDevice(deviceId: String): List<UsageLog> =
        _homeState.value.usageLogs.filter { it.deviceId == deviceId }

    override fun dismissAlert(alertId: String) {
        _alerts.value = _alerts.value.filter { it.id != alertId }
    }

    private inline fun updateDevice(deviceId: String, transform: (SmartDevice) -> SmartDevice) {
        val current = _homeState.value
        val updatedDevices = current.devices.map { device ->
            if (device.id == deviceId) transform(device) else device
        }
        _homeState.value = current.copy(devices = updatedDevices)
    }

    private fun logUsage(device: SmartDevice, startMillis: Long?, endMillis: Long) {
        if (startMillis == null) return
        val durationMinutes = ((endMillis - startMillis) / 60_000).toInt().coerceAtLeast(1)
        val log = UsageLog(
            id = UUID.randomUUID().toString(),
            deviceId = device.id,
            deviceName = device.name,
            startTimeMillis = startMillis,
            endTimeMillis = endMillis,
            durationMinutes = durationMinutes
        )
        val current = _homeState.value
        _homeState.value = current.copy(usageLogs = current.usageLogs + log)
    }

    private suspend fun runSafetyMonitor() {
        while (scope.isActive) {
            delay(10_000)
            val now = System.currentTimeMillis()
            val current = _homeState.value
            current.devices.filter { device ->
                device.isSafetyCritical &&
                    device.status == DeviceStatus.ON &&
                    device.turnedOnAtMillis != null &&
                    device.maxOnDurationMinutes != null
            }.forEach { device ->
                val elapsedMinutes = (now - device.turnedOnAtMillis!!) / 60_000
                if (elapsedMinutes >= device.maxOnDurationMinutes!!) {
                    forceOffWithAlert(device, now)
                }
            }
        }
    }

    private fun forceOffWithAlert(device: SmartDevice, now: Long) {
        logUsage(device, device.turnedOnAtMillis, now)
        updateDevice(device.id) {
            it.copy(status = DeviceStatus.OFF, turnedOnAtMillis = null)
        }
        val alert = SafetyAlert(
            id = UUID.randomUUID().toString(),
            deviceId = device.id,
            deviceName = device.name,
            message = "${device.name} was automatically switched OFF — max duration of ${device.maxOnDurationMinutes} min exceeded.",
            timestampMillis = now
        )
        _alerts.value = _alerts.value + alert
        val current = _homeState.value
        _homeState.value = current.copy(alerts = current.alerts + alert)
    }

    private suspend fun runLightScheduleWorker() {
        while (scope.isActive) {
            delay(60_000)
            val cal = java.util.Calendar.getInstance()
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = cal.get(java.util.Calendar.MINUTE)
            val current = _homeState.value
            current.devices.filter { it.lightSchedule?.enabled == true }.forEach { device ->
                val schedule = device.lightSchedule ?: return@forEach
                val shouldBeOn = isWithinSchedule(hour, minute, schedule)
                val targetStatus = if (shouldBeOn) DeviceStatus.ON else DeviceStatus.OFF
                if (device.status.isControllable && device.status != targetStatus) {
                    updateDevice(device.id) { d ->
                        if (targetStatus == DeviceStatus.ON) {
                            d.copy(status = targetStatus, turnedOnAtMillis = System.currentTimeMillis())
                        } else {
                            logUsage(d, d.turnedOnAtMillis, System.currentTimeMillis())
                            d.copy(status = targetStatus, turnedOnAtMillis = null)
                        }
                    }
                }
            }
        }
    }

    private fun isWithinSchedule(hour: Int, minute: Int, schedule: LightSchedule): Boolean {
        val current = hour * 60 + minute
        val onTime = schedule.turnOnHour * 60 + schedule.turnOnMinute
        val offTime = schedule.turnOffHour * 60 + schedule.turnOffMinute
        return if (onTime <= offTime) {
            current in onTime until offTime
        } else {
            current >= onTime || current < offTime
        }
    }
}
