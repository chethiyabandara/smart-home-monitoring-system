package com.scs3311.smart_home_monitoring_app.data.model

enum class DeviceStatus {
    ON, OFF, ERROR, DISCONNECTED;

    val isControllable: Boolean
        get() = this == ON || this == OFF
}

enum class DeviceType(val label: String, val icon: String) {
    OUTLET("Electrical Outlet", "outlet"),
    MULTI_SWITCH("Multi-Switch Unit", "switch"),
    LIGHT("Light", "light"),
    IRON("Iron (Safety)", "iron"),
    CAMERA("Security Camera", "camera")
}

data class SwitchState(
    val id: String,
    val name: String,
    val status: DeviceStatus
)

data class LightSchedule(
    val turnOnHour: Int,
    val turnOnMinute: Int,
    val turnOffHour: Int,
    val turnOffMinute: Int,
    val enabled: Boolean = true
) {
    fun formattedOn(): String = "%02d:%02d".format(turnOnHour, turnOnMinute)
    fun formattedOff(): String = "%02d:%02d".format(turnOffHour, turnOffMinute)
}

data class GridPosition(
    val row: Int,
    val col: Int
)

data class Room(
    val id: String,
    val name: String,
    val gridRow: Int,
    val gridCol: Int,
    val rowSpan: Int = 1,
    val colSpan: Int = 1
)

data class Floor(
    val id: String,
    val name: String,
    val rooms: List<Room>
)

data class SmartDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val roomId: String,
    val floorId: String,
    val status: DeviceStatus,
    val gridPosition: GridPosition,
    val switches: List<SwitchState> = emptyList(),
    val maxOnDurationMinutes: Int? = null,
    val turnedOnAtMillis: Long? = null,
    val lightSchedule: LightSchedule? = null,
    val cameraSnapshotUrl: String? = null,
    val isSafetyCritical: Boolean = false
)

data class UsageLog(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationMinutes: Int
)

data class SafetyAlert(
    val id: String,
    val deviceId: String,
    val deviceName: String,
    val message: String,
    val timestampMillis: Long
)

data class HomeState(
    val homeName: String,
    val floors: List<Floor>,
    val devices: List<SmartDevice>,
    val usageLogs: List<UsageLog>,
    val alerts: List<SafetyAlert>
)
