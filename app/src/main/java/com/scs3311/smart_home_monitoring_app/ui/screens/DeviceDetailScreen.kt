package com.scs3311.smart_home_monitoring_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.scs3311.smart_home_monitoring_app.data.model.DeviceStatus
import com.scs3311.smart_home_monitoring_app.data.model.DeviceType
import com.scs3311.smart_home_monitoring_app.data.model.LightSchedule
import com.scs3311.smart_home_monitoring_app.viewmodel.DeviceDetailViewModel
import com.scs3311.smart_home_monitoring_app.ui.components.DeviceTypeIcon
import com.scs3311.smart_home_monitoring_app.ui.components.StatusBadge
import com.scs3311.smart_home_monitoring_app.ui.components.statusColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    viewModel: DeviceDetailViewModel,
    onBack: () -> Unit
) {
    val device by viewModel.device.collectAsState()
    val usageLogs by viewModel.usageLogs.collectAsState()

    if (device == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Device not found")
        }
        return
    }

    val d = device!!
    var maxDuration by remember(d.maxOnDurationMinutes) {
        mutableFloatStateOf((d.maxOnDurationMinutes ?: 15).toFloat())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(d.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DeviceTypeIcon(type = d.type)
                    Text(d.type.label, style = MaterialTheme.typography.bodyMedium)
                    StatusBadge(d.status)
                    if (d.status.isControllable && d.type != DeviceType.CAMERA && d.type != DeviceType.MULTI_SWITCH) {
                        Button(
                            onClick = { viewModel.toggleDevice() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (d.status == DeviceStatus.ON) "Turn OFF" else "Turn ON")
                        }
                    }
                }
            }

            when (d.type) {
                DeviceType.MULTI_SWITCH -> MultiSwitchSection(d, viewModel)
                DeviceType.IRON -> IronSafetySection(d, maxDuration, viewModel) { maxDuration = it }
                DeviceType.LIGHT -> LightScheduleSection(d, viewModel)
                DeviceType.CAMERA -> CameraSection(d, viewModel)
                DeviceType.OUTLET -> { /* basic toggle only */ }
            }

            if (d.turnedOnAtMillis != null && d.status == DeviceStatus.ON) {
                val elapsed = System.currentTimeMillis() - d.turnedOnAtMillis
                val mins = TimeUnit.MILLISECONDS.toMinutes(elapsed)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Active for $mins minute(s)",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor(DeviceStatus.ON)
                    )
                }
            }

            if (usageLogs.isNotEmpty()) {
                Text("Recent Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                usageLogs.take(5).forEach { log ->
                    val fmt = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(fmt.format(Date(log.startTimeMillis)))
                            Text("${log.durationMinutes} min")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MultiSwitchSection(
    device: com.scs3311.smart_home_monitoring_app.data.model.SmartDevice,
    viewModel: DeviceDetailViewModel
) {
    Text("Switch Panel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    device.switches.forEach { sw ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(sw.name, fontWeight = FontWeight.Medium)
                    StatusBadge(sw.status)
                }
                if (sw.status.isControllable) {
                    Switch(
                        checked = sw.status == DeviceStatus.ON,
                        onCheckedChange = { viewModel.toggleSwitch(sw.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IronSafetySection(
    device: com.scs3311.smart_home_monitoring_app.data.model.SmartDevice,
    maxDuration: Float,
    viewModel: DeviceDetailViewModel,
    onDurationChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Safety Settings", fontWeight = FontWeight.Bold)
            Text(
                "Max ON duration: ${maxDuration.toInt()} minutes",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Device will auto-turn OFF when limit is exceeded (server-side safety rule).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = maxDuration,
                onValueChange = {
                    onDurationChange(it)
                    viewModel.updateMaxDuration(it.toInt())
                },
                valueRange = 1f..60f,
                steps = 58
            )
        }
    }
}

@Composable
private fun LightScheduleSection(
    device: com.scs3311.smart_home_monitoring_app.data.model.SmartDevice,
    viewModel: DeviceDetailViewModel
) {
    val schedule = device.lightSchedule ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Auto Schedule", fontWeight = FontWeight.Bold)
            Text("Turn ON: ${schedule.formattedOn()}")
            Text("Turn OFF: ${schedule.formattedOff()}")
            Text(
                if (schedule.enabled) "Schedule is active" else "Schedule disabled",
                color = if (schedule.enabled) statusColor(DeviceStatus.ON) else statusColor(DeviceStatus.OFF)
            )
            OutlinedButton(
                onClick = {
                    viewModel.updateSchedule(schedule.copy(enabled = !schedule.enabled))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (schedule.enabled) "Disable Schedule" else "Enable Schedule")
            }
        }
    }
}

@Composable
private fun CameraSection(
    device: com.scs3311.smart_home_monitoring_app.data.model.SmartDevice,
    viewModel: DeviceDetailViewModel
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Live Snapshot", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            AsyncImage(
                model = device.cameraSnapshotUrl,
                contentDescription = "Camera snapshot",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            OutlinedButton(
                onClick = { viewModel.refreshCamera() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("Refresh Snapshot", modifier = Modifier.padding(start = 8.dp))
            }
            StatusBadge(
                status = if (device.status == DeviceStatus.ON) DeviceStatus.ON else DeviceStatus.DISCONNECTED,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
