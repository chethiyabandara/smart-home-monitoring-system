package com.scs3311.smart_home_monitoring_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scs3311.smart_home_monitoring_app.data.model.DeviceStatus
import com.scs3311.smart_home_monitoring_app.ui.theme.StatusDisconnected
import com.scs3311.smart_home_monitoring_app.ui.theme.StatusError
import com.scs3311.smart_home_monitoring_app.ui.theme.StatusOff
import com.scs3311.smart_home_monitoring_app.ui.theme.StatusOn

@Composable
fun StatusBadge(status: DeviceStatus, modifier: Modifier = Modifier) {
    val (color, label) = when (status) {
        DeviceStatus.ON -> StatusOn to "ON"
        DeviceStatus.OFF -> StatusOff to "OFF"
        DeviceStatus.ERROR -> StatusError to "ERROR"
        DeviceStatus.DISCONNECTED -> StatusDisconnected to "DISCONNECTED"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

fun statusColor(status: DeviceStatus): Color = when (status) {
    DeviceStatus.ON -> StatusOn
    DeviceStatus.OFF -> StatusOff
    DeviceStatus.ERROR -> StatusError
    DeviceStatus.DISCONNECTED -> StatusDisconnected
}
