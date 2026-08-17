package com.scs3311.smart_home_monitoring_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scs3311.smart_home_monitoring_app.data.model.DeviceCreationRequest
import com.scs3311.smart_home_monitoring_app.data.model.DeviceStatus
import com.scs3311.smart_home_monitoring_app.data.model.DeviceType
import com.scs3311.smart_home_monitoring_app.data.model.Floor
import com.scs3311.smart_home_monitoring_app.data.model.GridPosition
import com.scs3311.smart_home_monitoring_app.data.model.LightSchedule
import com.scs3311.smart_home_monitoring_app.data.model.Room

private enum class HomeSetupMode { FLOOR, ROOM, DEVICE }

@Composable
fun HomeManagementCard(
    floors: List<Floor>,
    selectedFloorId: String,
    onSelectFloor: (String) -> Unit,
    onCreateFloor: (String) -> Unit,
    onCreateRoom: (String, String, Int, Int, Int, Int) -> Unit,
    onCreateDevice: (DeviceCreationRequest) -> Unit
) {
    var mode by remember { mutableStateOf(HomeSetupMode.FLOOR) }

    var floorName by remember { mutableStateOf("") }

    var roomFloorId by remember(selectedFloorId, floors) {
        mutableStateOf(selectedFloorId.ifEmpty { floors.firstOrNull()?.id.orEmpty() })
    }
    var roomName by remember { mutableStateOf("") }
    var roomGridRow by remember { mutableStateOf("0") }
    var roomGridCol by remember { mutableStateOf("0") }
    var roomRowSpan by remember { mutableStateOf("1") }
    var roomColSpan by remember { mutableStateOf("1") }

    var deviceFloorId by remember(selectedFloorId, floors) {
        mutableStateOf(selectedFloorId.ifEmpty { floors.firstOrNull()?.id.orEmpty() })
    }
    var deviceRoomId by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf(DeviceType.OUTLET) }
    var deviceGridRow by remember { mutableStateOf("0") }
    var deviceGridCol by remember { mutableStateOf("0") }
    var safetyCritical by remember { mutableStateOf(false) }
    var maxDuration by remember { mutableStateOf("15") }
    var lightOnTime by remember { mutableStateOf("18:00") }
    var lightOffTime by remember { mutableStateOf("22:00") }
    var lightEnabled by remember { mutableStateOf(true) }
    var switchNames by remember { mutableStateOf("Main Light,Desk Lamp,Fan") }
    var cameraSnapshotUrl by remember { mutableStateOf("") }

    val roomOptions = floors.find { it.id == deviceFloorId }?.rooms.orEmpty()

    LaunchedEffect(deviceFloorId, roomOptions) {
        if (deviceRoomId.isBlank() || roomOptions.none { it.id == deviceRoomId }) {
            deviceRoomId = roomOptions.firstOrNull()?.id.orEmpty()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Home Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == HomeSetupMode.FLOOR, onClick = { mode = HomeSetupMode.FLOOR }, label = { Text("Floor") })
                FilterChip(selected = mode == HomeSetupMode.ROOM, onClick = { mode = HomeSetupMode.ROOM }, label = { Text("Room") })
                FilterChip(selected = mode == HomeSetupMode.DEVICE, onClick = { mode = HomeSetupMode.DEVICE }, label = { Text("Device") })
            }

            when (mode) {
                HomeSetupMode.FLOOR -> {
                    OutlinedTextField(
                        value = floorName,
                        onValueChange = { floorName = it },
                        label = { Text("Floor name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (floorName.isNotBlank()) {
                                onCreateFloor(floorName.trim())
                                floorName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Floor")
                    }
                }

                HomeSetupMode.ROOM -> {
                    FloorPicker(floors = floors, selectedFloorId = roomFloorId, onSelectFloor = { roomFloorId = it })
                    OutlinedTextField(value = roomName, onValueChange = { roomName = it }, label = { Text("Room name") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallNumberField(value = roomGridRow, onValueChange = { roomGridRow = it }, label = "Row", modifier = Modifier.weight(1f))
                        SmallNumberField(value = roomGridCol, onValueChange = { roomGridCol = it }, label = "Col", modifier = Modifier.weight(1f))
                        SmallNumberField(value = roomRowSpan, onValueChange = { roomRowSpan = it }, label = "Rows", modifier = Modifier.weight(1f))
                        SmallNumberField(value = roomColSpan, onValueChange = { roomColSpan = it }, label = "Cols", modifier = Modifier.weight(1f))
                    }
                    Button(
                        enabled = roomFloorId.isNotBlank() && roomName.isNotBlank(),
                        onClick = {
                            onCreateRoom(
                                roomFloorId,
                                roomName.trim(),
                                roomGridRow.toIntOrNull() ?: 0,
                                roomGridCol.toIntOrNull() ?: 0,
                                roomRowSpan.toIntOrNull() ?: 1,
                                roomColSpan.toIntOrNull() ?: 1
                            )
                            roomName = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Room")
                    }
                }

                HomeSetupMode.DEVICE -> {
                    FloorPicker(floors = floors, selectedFloorId = deviceFloorId, onSelectFloor = { deviceFloorId = it })
                    RoomPicker(rooms = roomOptions, selectedRoomId = deviceRoomId, onSelectRoom = { deviceRoomId = it })
                    OutlinedTextField(value = deviceName, onValueChange = { deviceName = it }, label = { Text("Device name") }, modifier = Modifier.fillMaxWidth())
                    TypePicker(selected = deviceType, onSelect = { deviceType = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallNumberField(value = deviceGridRow, onValueChange = { deviceGridRow = it }, label = "Grid row", modifier = Modifier.weight(1f))
                        SmallNumberField(value = deviceGridCol, onValueChange = { deviceGridCol = it }, label = "Grid col", modifier = Modifier.weight(1f))
                    }

                    when (deviceType) {
                        DeviceType.IRON -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallNumberField(
                                    value = maxDuration,
                                    onValueChange = { maxDuration = it },
                                    label = "Max minutes",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Safety critical", modifier = Modifier.weight(1f))
                                    Switch(checked = safetyCritical, onCheckedChange = { safetyCritical = it })
                                }
                            }
                        }

                        DeviceType.LIGHT -> {
                            OutlinedTextField(value = lightOnTime, onValueChange = { lightOnTime = it }, label = { Text("On time HH:MM") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = lightOffTime, onValueChange = { lightOffTime = it }, label = { Text("Off time HH:MM") }, modifier = Modifier.fillMaxWidth())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(checked = lightEnabled, onCheckedChange = { lightEnabled = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable schedule")
                            }
                        }

                        DeviceType.MULTI_SWITCH -> {
                            OutlinedTextField(
                                value = switchNames,
                                onValueChange = { switchNames = it },
                                label = { Text("Switch names, comma separated") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        DeviceType.CAMERA -> {
                            OutlinedTextField(value = cameraSnapshotUrl, onValueChange = { cameraSnapshotUrl = it }, label = { Text("Snapshot URL") }, modifier = Modifier.fillMaxWidth())
                        }

                        DeviceType.OUTLET -> Unit
                    }

                    Button(
                        enabled = deviceName.isNotBlank() && deviceFloorId.isNotBlank() && deviceRoomId.isNotBlank(),
                        onClick = {
                            onCreateDevice(
                                DeviceCreationRequest(
                                    name = deviceName.trim(),
                                    type = deviceType,
                                    floorId = deviceFloorId,
                                    roomId = deviceRoomId,
                                    gridPosition = GridPosition(deviceGridRow.toIntOrNull() ?: 0, deviceGridCol.toIntOrNull() ?: 0),
                                    status = if (deviceType == DeviceType.CAMERA) DeviceStatus.ON else DeviceStatus.OFF,
                                    switchNames = if (deviceType == DeviceType.MULTI_SWITCH) switchNames.split(",").map { it.trim() }.filter { it.isNotBlank() } else emptyList(),
                                    maxOnDurationMinutes = if (deviceType == DeviceType.IRON) maxDuration.toIntOrNull() ?: 15 else null,
                                    lightSchedule = if (deviceType == DeviceType.LIGHT) parseSchedule(lightOnTime, lightOffTime, lightEnabled) else null,
                                    cameraSnapshotUrl = cameraSnapshotUrl.takeIf { it.isNotBlank() },
                                    isSafetyCritical = deviceType == DeviceType.IRON && safetyCritical
                                )
                            )
                            deviceName = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Device")
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FloorPicker(floors: List<Floor>, selectedFloorId: String, onSelectFloor: (String) -> Unit) {
    if (floors.isEmpty()) {
        Text("Add a floor first to continue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Floor", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            floors.forEach { floor ->
                FilterChip(
                    selected = floor.id == selectedFloorId,
                    onClick = { onSelectFloor(floor.id) },
                    label = { Text(floor.name) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RoomPicker(rooms: List<Room>, selectedRoomId: String, onSelectRoom: (String) -> Unit) {
    if (rooms.isEmpty()) {
        Text("Add a room to the selected floor before adding devices.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Room", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rooms.forEach { room ->
                FilterChip(
                    selected = room.id == selectedRoomId,
                    onClick = { onSelectRoom(room.id) },
                    label = { Text(room.name) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TypePicker(selected: DeviceType, onSelect: (DeviceType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Device type", fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DeviceType.values().forEach { type ->
                FilterChip(selected = type == selected, onClick = { onSelect(type) }, label = { Text(type.label) })
            }
        }
    }
}

@Composable
private fun SmallNumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { char -> char.isDigit() }) },
        label = { Text(label) },
        modifier = modifier
    )
}

private fun parseSchedule(start: String, end: String, enabled: Boolean): LightSchedule? {
    val startParts = start.split(":").mapNotNull { it.toIntOrNull() }
    val endParts = end.split(":").mapNotNull { it.toIntOrNull() }
    if (startParts.size != 2 || endParts.size != 2) return null
    return LightSchedule(startParts[0], startParts[1], endParts[0], endParts[1], enabled)
}
