package com.scs3311.smart_home_monitoring_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scs3311.smart_home_monitoring_app.data.model.LightSchedule
import com.scs3311.smart_home_monitoring_app.data.model.SmartDevice
import com.scs3311.smart_home_monitoring_app.data.model.UsageLog
import com.scs3311.smart_home_monitoring_app.data.repository.SmartHomeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DeviceDetailViewModel(
    private val repository: SmartHomeRepository,
    private val deviceId: String
) : ViewModel() {

    val device: StateFlow<SmartDevice?> = repository.homeState
        .map { state -> state.devices.find { it.id == deviceId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.getDevice(deviceId))

    val usageLogs: StateFlow<List<UsageLog>> = repository.homeState
        .map { repository.getUsageForDevice(deviceId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.getUsageForDevice(deviceId))

    fun toggleDevice() {
        viewModelScope.launch { repository.toggleDevice(deviceId) }
    }

    fun toggleSwitch(switchId: String) {
        viewModelScope.launch { repository.toggleSwitch(deviceId, switchId) }
    }

    fun updateMaxDuration(minutes: Int) {
        viewModelScope.launch { repository.updateMaxOnDuration(deviceId, minutes) }
    }

    fun updateSchedule(schedule: LightSchedule) {
        viewModelScope.launch { repository.updateLightSchedule(deviceId, schedule) }
    }

    fun refreshCamera() {
        viewModelScope.launch { repository.refreshCameraSnapshot(deviceId) }
    }

    class Factory(
        private val repository: SmartHomeRepository,
        private val deviceId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DeviceDetailViewModel::class.java)) {
                return DeviceDetailViewModel(repository, deviceId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
