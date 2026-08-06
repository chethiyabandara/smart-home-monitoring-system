package com.scs3311.smart_home_monitoring_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scs3311.smart_home_monitoring_app.viewmodel.HomeViewModel
import com.scs3311.smart_home_monitoring_app.ui.components.DeviceCard
import com.scs3311.smart_home_monitoring_app.ui.components.FloorPlanGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDeviceClick: (String) -> Unit,
    onUsageClick: () -> Unit
) {
    val homeState by viewModel.homeState.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    var selectedFloorId by remember { mutableStateOf(homeState.floors.firstOrNull()?.id ?: "") }

    if (selectedFloorId.isEmpty() && homeState.floors.isNotEmpty()) {
        selectedFloorId = homeState.floors.first().id
    }

    val floorDevices = homeState.devices.filter { it.floorId == selectedFloorId }
    val floor = homeState.floors.find { it.id == selectedFloorId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = homeState.homeName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Smart Home Monitoring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onUsageClick) {
                        Icon(Icons.Default.BarChart, contentDescription = "Usage Reports")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (alerts.isNotEmpty()) {
                item {
                    AlertBanner(
                        message = alerts.last().message,
                        onDismiss = { viewModel.dismissAlert(alerts.last().id) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    homeState.floors.forEach { f ->
                        FilterChip(
                            selected = f.id == selectedFloorId,
                            onClick = { selectedFloorId = f.id },
                            label = { Text(f.name) },
                            leadingIcon = if (f.id == selectedFloorId) {
                                { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.height(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            if (floor != null) {
                item {
                    FloorPlanGrid(
                        rooms = floor.rooms,
                        devices = floorDevices,
                        onDeviceClick = onDeviceClick
                    )
                }
            }

            item {
                Text(
                    text = "Devices (${floorDevices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(floorDevices, key = { it.id }) { device ->
                DeviceCard(
                    device = device,
                    onClick = { onDeviceClick(device.id) },
                    onToggle = { viewModel.toggleDevice(device.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AlertBanner(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}
