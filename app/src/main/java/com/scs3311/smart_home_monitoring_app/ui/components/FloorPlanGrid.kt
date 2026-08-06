package com.scs3311.smart_home_monitoring_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scs3311.smart_home_monitoring_app.data.model.Room
import com.scs3311.smart_home_monitoring_app.data.model.SmartDevice

@Composable
fun FloorPlanGrid(
    rooms: List<Room>,
    devices: List<SmartDevice>,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridRows = (rooms.maxOfOrNull { it.gridRow + it.rowSpan } ?: 2).coerceAtLeast(2)
    val gridCols = (rooms.maxOfOrNull { it.gridCol + it.colSpan } ?: 3).coerceAtLeast(2)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Floor Plan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        for (row in 0 until gridRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until gridCols) {
                    val room = rooms.find { r ->
                        row in r.gridRow until (r.gridRow + r.rowSpan) &&
                            col in r.gridCol until (r.gridCol + r.colSpan) &&
                            row == r.gridRow && col == r.gridCol
                    }
                    if (room != null) {
                        RoomCell(
                            room = room,
                            devices = devices.filter { it.roomId == room.id },
                            onDeviceClick = onDeviceClick,
                            modifier = Modifier
                                .weight(room.colSpan.toFloat())
                                .height((80 * room.rowSpan).dp)
                        )
                    } else if (rooms.none { r ->
                            row in r.gridRow until (r.gridRow + r.rowSpan) &&
                                col in r.gridCol until (r.gridCol + r.colSpan)
                        }) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomCell(
    room: Room,
    devices: List<SmartDevice>,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = room.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            devices.take(4).forEach { device ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(statusColor(device.status).copy(alpha = 0.2f))
                        .clickable { onDeviceClick(device.id) },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = deviceIcon(device.type),
                        contentDescription = device.name,
                        modifier = Modifier.size(16.dp),
                        tint = statusColor(device.status)
                    )
                }
            }
            if (devices.size > 4) {
                Text(
                    text = "+${devices.size - 4}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
        if (devices.isEmpty()) {
            Text(
                text = "No devices",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
