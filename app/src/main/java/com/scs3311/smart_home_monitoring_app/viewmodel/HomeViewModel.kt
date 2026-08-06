package com.scs3311.smart_home_monitoring_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.scs3311.smart_home_monitoring_app.data.model.Floor
import com.scs3311.smart_home_monitoring_app.data.model.HomeState
import com.scs3311.smart_home_monitoring_app.data.model.SafetyAlert
import com.scs3311.smart_home_monitoring_app.data.model.SmartDevice
import com.scs3311.smart_home_monitoring_app.data.repository.SmartHomeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: SmartHomeRepository) : ViewModel() {

    val homeState: StateFlow<HomeState> = repository.homeState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repository.homeState.value)

    val alerts: StateFlow<List<SafetyAlert>> = repository.alerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var selectedFloorId: String = homeState.value.floors.firstOrNull()?.id ?: ""
        private set

    fun selectFloor(floorId: String) {
        selectedFloorId = floorId
    }

    fun devicesForFloor(floorId: String): StateFlow<List<SmartDevice>> =
        homeState.map { state -> state.devices.filter { it.floorId == floorId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun floorById(floorId: String): Floor? =
        homeState.value.floors.find { it.id == floorId }

    fun toggleDevice(deviceId: String) {
        viewModelScope.launch { repository.toggleDevice(deviceId) }
    }

    fun dismissAlert(alertId: String) {
        repository.dismissAlert(alertId)
    }

    class Factory(private val repository: SmartHomeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
