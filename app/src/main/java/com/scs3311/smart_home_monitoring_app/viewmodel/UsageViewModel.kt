package com.scs3311.smart_home_monitoring_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scs3311.smart_home_monitoring_app.data.model.DeviceType
import com.scs3311.smart_home_monitoring_app.data.model.UsageLog
import com.scs3311.smart_home_monitoring_app.data.repository.SmartHomeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DeviceUsageSummary(
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val todayMinutes: Int,
    val weekMinutes: Int,
    val totalSessions: Int
)

class UsageViewModel(private val repository: SmartHomeRepository) : ViewModel() {

    val summaries: StateFlow<List<DeviceUsageSummary>> = repository.homeState
        .map { state -> buildSummaries(state.usageLogs, state.devices.map { it.id to it }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allLogs: StateFlow<List<UsageLog>> = repository.homeState
        .map { it.usageLogs.sortedByDescending { log -> log.startTimeMillis } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildSummaries(
        logs: List<UsageLog>,
        devices: List<Pair<String, com.scs3311.smart_home_monitoring_app.data.model.SmartDevice>>
    ): List<DeviceUsageSummary> {
        val now = System.currentTimeMillis()
        val dayStart = now - 24 * 3_600_000L
        val weekStart = now - 7 * 24 * 3_600_000L

        return devices
            .filter { (_, device) ->
                device.type in listOf(DeviceType.IRON, DeviceType.OUTLET, DeviceType.LIGHT, DeviceType.MULTI_SWITCH)
            }
            .map { (id, device) ->
                val deviceLogs = logs.filter { it.deviceId == id }
                DeviceUsageSummary(
                    deviceId = id,
                    deviceName = device.name,
                    deviceType = device.type,
                    todayMinutes = deviceLogs.filter { it.startTimeMillis >= dayStart }
                        .sumOf { it.durationMinutes },
                    weekMinutes = deviceLogs.filter { it.startTimeMillis >= weekStart }
                        .sumOf { it.durationMinutes },
                    totalSessions = deviceLogs.size
                )
            }
            .sortedByDescending { it.weekMinutes }
    }

    class Factory(private val repository: SmartHomeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UsageViewModel::class.java)) {
                return UsageViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
