package com.scs3311.smart_home_monitoring_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scs3311.smart_home_monitoring_app.viewmodel.UsageViewModel
import com.scs3311.smart_home_monitoring_app.ui.components.DeviceTypeIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageReportScreen(
    viewModel: UsageViewModel,
    onBack: () -> Unit
) {
    val summaries by viewModel.summaries.collectAsState()
    val logs by viewModel.allLogs.collectAsState()
    val maxWeek = summaries.maxOfOrNull { it.weekMinutes }?.coerceAtLeast(1) ?: 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Device Usage Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(summaries) { summary ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DeviceTypeIcon(type = summary.deviceType)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.deviceName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Today: ${summary.todayMinutes} min · Week: ${summary.weekMinutes} min",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "${summary.totalSessions} sessions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        LinearProgressIndicator(
                            progress = { summary.weekMinutes.toFloat() / maxWeek },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    "Activity Log",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(logs) { log ->
                val fmt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(log.deviceName, fontWeight = FontWeight.Medium)
                            Text(
                                fmt.format(Date(log.startTimeMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("${log.durationMinutes} min", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
