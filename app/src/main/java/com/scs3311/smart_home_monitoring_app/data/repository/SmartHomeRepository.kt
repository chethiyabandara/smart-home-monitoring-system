package com.scs3311.smart_home_monitoring_app.data.repository

import com.scs3311.smart_home_monitoring_app.data.model.DeviceStatus
import com.scs3311.smart_home_monitoring_app.data.model.HomeState
import com.scs3311.smart_home_monitoring_app.data.model.LightSchedule
import com.scs3311.smart_home_monitoring_app.data.model.SafetyAlert
import com.scs3311.smart_home_monitoring_app.data.model.SmartDevice
import com.scs3311.smart_home_monitoring_app.data.model.UsageLog
import kotlinx.coroutines.flow.StateFlow

interface SmartHomeRepository {
    val homeState: StateFlow<HomeState>
    val alerts: StateFlow<List<SafetyAlert>>

    suspend fun toggleDevice(deviceId: String)
    suspend fun toggleSwitch(deviceId: String, switchId: String)
    suspend fun updateMaxOnDuration(deviceId: String, minutes: Int)
    suspend fun updateLightSchedule(deviceId: String, schedule: LightSchedule)
    suspend fun refreshCameraSnapshot(deviceId: String)
    fun getDevice(deviceId: String): SmartDevice?
    fun getUsageForDevice(deviceId: String): List<UsageLog>
    fun dismissAlert(alertId: String)
}
