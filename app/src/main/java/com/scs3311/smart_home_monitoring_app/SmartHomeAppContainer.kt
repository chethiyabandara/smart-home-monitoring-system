package com.scs3311.smart_home_monitoring_app

import com.scs3311.smart_home_monitoring_app.data.repository.FirebaseSmartHomeRepository
import com.scs3311.smart_home_monitoring_app.data.repository.SmartHomeRepository

object SmartHomeAppContainer {
    val repository: SmartHomeRepository by lazy { FirebaseSmartHomeRepository() }
}
