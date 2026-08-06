package com.scs3311.smart_home_monitoring_app.ui.navigation

object Routes {
    const val HOME = "home"
    const val DEVICE_DETAIL = "device/{deviceId}"
    const val USAGE = "usage"

    fun deviceDetail(deviceId: String) = "device/$deviceId"
}
