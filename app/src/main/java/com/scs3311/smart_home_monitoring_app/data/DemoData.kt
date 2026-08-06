package com.scs3311.smart_home_monitoring_app.data

import com.scs3311.smart_home_monitoring_app.data.model.DeviceStatus
import com.scs3311.smart_home_monitoring_app.data.model.DeviceType
import com.scs3311.smart_home_monitoring_app.data.model.Floor
import com.scs3311.smart_home_monitoring_app.data.model.GridPosition
import com.scs3311.smart_home_monitoring_app.data.model.HomeState
import com.scs3311.smart_home_monitoring_app.data.model.LightSchedule
import com.scs3311.smart_home_monitoring_app.data.model.Room
import com.scs3311.smart_home_monitoring_app.data.model.SmartDevice
import com.scs3311.smart_home_monitoring_app.data.model.SwitchState
import com.scs3311.smart_home_monitoring_app.data.model.UsageLog

object DemoData {

    const val HOME_NAME = "My Smart Home"

    val initialHome: HomeState = HomeState(
        homeName = HOME_NAME,
        floors = listOf(
            Floor(
                id = "floor_ground",
                name = "Ground Floor",
                rooms = listOf(
                    Room("room_living", "Living Room", gridRow = 0, gridCol = 0, rowSpan = 2, colSpan = 2),
                    Room("room_kitchen", "Kitchen", gridRow = 0, gridCol = 2, rowSpan = 1, colSpan = 1),
                    Room("room_bath_g", "Bathroom", gridRow = 1, gridCol = 2, rowSpan = 1, colSpan = 1)
                )
            ),
            Floor(
                id = "floor_first",
                name = "First Floor",
                rooms = listOf(
                    Room("room_bed1", "Bedroom 1", gridRow = 0, gridCol = 0, rowSpan = 1, colSpan = 2),
                    Room("room_bed2", "Bedroom 2", gridRow = 1, gridCol = 0, rowSpan = 1, colSpan = 1),
                    Room("room_hall", "Hall", gridRow = 1, gridCol = 1, rowSpan = 1, colSpan = 1)
                )
            )
        ),
        devices = listOf(
            SmartDevice(
                id = "dev_living_light",
                name = "Living Room Light",
                type = DeviceType.LIGHT,
                roomId = "room_living",
                floorId = "floor_ground",
                status = DeviceStatus.OFF,
                gridPosition = GridPosition(0, 0),
                lightSchedule = LightSchedule(18, 0, 23, 0)
            ),
            SmartDevice(
                id = "dev_living_panel",
                name = "Living Room Switch Panel",
                type = DeviceType.MULTI_SWITCH,
                roomId = "room_living",
                floorId = "floor_ground",
                status = DeviceStatus.OFF,
                gridPosition = GridPosition(1, 1),
                switches = listOf(
                    SwitchState("sw1", "Main Light", DeviceStatus.OFF),
                    SwitchState("sw2", "Ceiling Fan", DeviceStatus.OFF),
                    SwitchState("sw3", "TV Outlet", DeviceStatus.ON)
                )
            ),
            SmartDevice(
                id = "dev_living_cam",
                name = "Living Room Camera",
                type = DeviceType.CAMERA,
                roomId = "room_living",
                floorId = "floor_ground",
                status = DeviceStatus.ON,
                gridPosition = GridPosition(0, 1),
                cameraSnapshotUrl = "https://picsum.photos/seed/livingroom/800/600"
            ),
            SmartDevice(
                id = "dev_kitchen_outlet1",
                name = "Fridge Outlet",
                type = DeviceType.OUTLET,
                roomId = "room_kitchen",
                floorId = "floor_ground",
                status = DeviceStatus.ON,
                gridPosition = GridPosition(0, 0)
            ),
            SmartDevice(
                id = "dev_kitchen_outlet2",
                name = "Microwave Outlet",
                type = DeviceType.OUTLET,
                roomId = "room_kitchen",
                floorId = "floor_ground",
                status = DeviceStatus.OFF,
                gridPosition = GridPosition(0, 1)
            ),
            SmartDevice(
                id = "dev_bath_light",
                name = "Bathroom Light",
                type = DeviceType.LIGHT,
                roomId = "room_bath_g",
                floorId = "floor_ground",
                status = DeviceStatus.OFF,
                gridPosition = GridPosition(0, 0)
            ),
            SmartDevice(
                id = "dev_bed1_light",
                name = "Bedroom 1 Light",
                type = DeviceType.LIGHT,
                roomId = "room_bed1",
                floorId = "floor_first",
                status = DeviceStatus.OFF,
                gridPosition = GridPosition(0, 0),
                lightSchedule = LightSchedule(6, 0, 22, 30)
            ),
            SmartDevice(
                id = "dev_bed1_iron",
                name = "Clothing Iron",
                type = DeviceType.IRON,
                roomId = "room_bed1",
                floorId = "floor_first",
                status = DeviceStatus.OFF,
                gridPosition = GridPosition(0, 1),
                maxOnDurationMinutes = 15,
                isSafetyCritical = true
            ),
            SmartDevice(
                id = "dev_bed1_outlet",
                name = "Bedroom Outlet",
                type = DeviceType.OUTLET,
                roomId = "room_bed1",
                floorId = "floor_first",
                status = DeviceStatus.DISCONNECTED,
                gridPosition = GridPosition(0, 2)
            ),
            SmartDevice(
                id = "dev_bed2_panel",
                name = "Bedroom 2 Switch Panel",
                type = DeviceType.MULTI_SWITCH,
                roomId = "room_bed2",
                floorId = "floor_first",
                status = DeviceStatus.OFF,
                gridPosition = GridPosition(0, 0),
                switches = listOf(
                    SwitchState("sw1", "Main Light", DeviceStatus.OFF),
                    SwitchState("sw2", "Desk Lamp", DeviceStatus.OFF),
                    SwitchState("sw3", "Fan", DeviceStatus.OFF),
                    SwitchState("sw4", "Heater", DeviceStatus.OFF),
                    SwitchState("sw5", "USB Charger", DeviceStatus.ON)
                )
            ),
            SmartDevice(
                id = "dev_bed2_light",
                name = "Bedroom 2 Light",
                type = DeviceType.LIGHT,
                roomId = "room_bed2",
                floorId = "floor_first",
                status = DeviceStatus.ERROR,
                gridPosition = GridPosition(0, 1)
            ),
            SmartDevice(
                id = "dev_hall_cam",
                name = "Hall Camera",
                type = DeviceType.CAMERA,
                roomId = "room_hall",
                floorId = "floor_first",
                status = DeviceStatus.ON,
                gridPosition = GridPosition(0, 0),
                cameraSnapshotUrl = "https://picsum.photos/seed/hallcam/800/600"
            ),
            SmartDevice(
                id = "dev_hall_outlet",
                name = "Hall Outlet",
                type = DeviceType.OUTLET,
                roomId = "room_hall",
                floorId = "floor_first",
                status = DeviceStatus.OFF,
                gridPosition = GridPosition(0, 1)
            )
        ),
        usageLogs = listOf(
            UsageLog("log1", "dev_bed1_iron", "Clothing Iron", nowMinusHours(2), nowMinusHours(2) + 25 * 60_000L, 25),
            UsageLog("log2", "dev_bed1_iron", "Clothing Iron", nowMinusHours(5), nowMinusHours(5) + 15 * 60_000L, 15),
            UsageLog("log3", "dev_kitchen_outlet1", "Fridge Outlet", nowMinusHours(24), nowMinusHours(20), 240),
            UsageLog("log4", "dev_living_panel", "Living Room Switch Panel", nowMinusHours(3), nowMinusHours(2), 60),
            UsageLog("log5", "dev_bed1_light", "Bedroom 1 Light", nowMinusHours(12), nowMinusHours(8), 240)
        ),
        alerts = emptyList()
    )

    private fun nowMinusHours(hours: Int): Long =
        System.currentTimeMillis() - hours * 3_600_000L
}
