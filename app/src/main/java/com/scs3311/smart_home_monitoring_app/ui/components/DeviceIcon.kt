package com.scs3311.smart_home_monitoring_app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Outlet
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.scs3311.smart_home_monitoring_app.data.model.DeviceType

fun deviceIcon(type: DeviceType): ImageVector = when (type) {
    DeviceType.OUTLET -> Icons.Default.Outlet
    DeviceType.MULTI_SWITCH -> Icons.Default.ToggleOn
    DeviceType.LIGHT -> Icons.Default.Lightbulb
    DeviceType.IRON -> Icons.Default.LocalLaundryService
    DeviceType.CAMERA -> Icons.Default.CameraAlt
}

@Composable
fun DeviceTypeIcon(type: DeviceType) {
    androidx.compose.material3.Icon(
        imageVector = deviceIcon(type),
        contentDescription = type.label
    )
}
