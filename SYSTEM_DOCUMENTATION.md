# Smart Home Monitoring System — Detailed Technical Guide

## 1. System purpose

This project is a smart-home monitoring system with two clients connected to the same Firebase Realtime Database home record:

- **Android application** (`smart-monitering-system`): the primary Compose application used to view the house, add floors/rooms/devices, control devices, and read alerts and usage reports.
- **Web simulator** (`simulator-monitoring/simulator`): a React/Vite control panel that can create and control the same records. It is useful for testing the Android application's real-time behaviour.

The two applications exchange no direct messages. They work together because both listen to and write to the Firebase location below:

```text
homes/home1
├── name
├── floors
├── rooms
├── devices
├── logs
└── alerts
```

## 2. Architecture and data flow

```text
                   ┌─────────────────────────────────┐
                   │ Firebase Realtime Database       │
                   │          homes/home1             │
                   └───────────────┬─────────────────┘
                                   │ real-time updates
            ┌──────────────────────┴──────────────────────┐
            │                                             │
┌───────────▼───────────┐                     ┌───────────▼───────────┐
│ Android Compose app    │                     │ React web simulator   │
│ Firebase repository    │                     │ Firebase onValue()    │
└───────────┬───────────┘                     └───────────┬───────────┘
            │ StateFlow                                      │ React state
┌───────────▼───────────┐                     ┌───────────▼───────────┐
│ Android ViewModels     │                     │ App / admin components │
└───────────┬───────────┘                     └───────────┬───────────┘
            │ Compose state                                  │ user events
┌───────────▼───────────┐                     ┌───────────▼───────────┐
│ Screens and components │                     │ Firebase update()      │
└───────────────────────┘                     └───────────────────────┘
```

### A typical device toggle

1. The user presses a device switch in Android or the simulator.
2. The screen calls a ViewModel or local React callback.
3. The callback writes the new status to `homes/home1/devices/{deviceId}`.
4. Firebase distributes the changed record to every active listener.
5. Android rebuilds `HomeState`; the simulator updates its `home` React state.
6. Both interfaces redraw with the new status.

### Iron safety workflow

1. An iron has `isSafetyCritical = true`, `maxOnDurationMinutes`, and `turnedOnAtMillis`.
2. When it turns on, the repository stores the current time in `turnedOnAtMillis`.
3. The Android Firebase repository's safety monitor checks the device periodically.
4. Once the maximum duration is exceeded, it turns the device off, records a usage log, and creates an alert.
5. Both applications receive the off state and alert in real time.

## 3. Android application

### 3.1 Startup, dependency setup, and navigation

#### `MainActivity.kt`

`MainActivity` is the Android entry activity.

- `onCreate(savedInstanceState)` is Android's startup callback. It enables edge-to-edge display, performs a small Firebase connection test, and installs the Compose UI tree.
- `testReference` is a local value pointing at the Firebase `connectionTest` record. It is used only to verify/log Firebase access.
- `lastAndroidWrite` receives the current timestamp, proving the app can write.
- `onDataChange(snapshot)` logs the connection-test value when Firebase returns it.
- `onCancelled(error)` logs a Firebase read error.
- `setContent { ... }` applies `SmarthomemonitoringappTheme`, creates the root `Surface`, and starts `SmartHomeNavGraph`.

#### `SmartHomeAppContainer.kt`

`SmartHomeAppContainer` is a singleton dependency container.

- `repository` is a global lazily-created `SmartHomeRepository`. It currently creates `FirebaseSmartHomeRepository`, so the whole Android app shares one Firebase-backed source of truth.
- `by lazy` means the repository is not created until the first screen asks for it.

#### `ui/navigation/Routes.kt`

`Routes` centralizes navigation strings.

- `HOME = "home"`: dashboard route.
- `DEVICE_DETAIL = "device/{deviceId}"`: route template for a device page.
- `USAGE = "usage"`: usage-report route.
- `deviceDetail(deviceId)` replaces the route placeholder with a real device ID, for example `device/dev_bed1_iron`.

#### `ui/navigation/SmartHomeNavGraph.kt`

`SmartHomeNavGraph(navController)` defines the app's navigation graph.

- `navController` is the navigation state holder; a default controller is created by `rememberNavController()`.
- `repository` receives the shared container repository.
- `NavHost` starts at `Routes.HOME`.
- The home destination constructs `HomeViewModel`, shows `HomeScreen`, and navigates to a chosen device or to usage.
- The detail destination reads the `deviceId` navigation argument, creates `DeviceDetailViewModel`, and shows `DeviceDetailScreen`.
- The usage destination creates `UsageViewModel` and shows `UsageReportScreen`.

### 3.2 Shared data models

#### `data/model/DeviceModels.kt`

This file defines the in-memory Kotlin representation of Firebase records.

- `DeviceStatus`: the possible runtime states.
  - `ON` and `OFF` are controllable.
  - `ERROR` and `DISCONNECTED` are display-only states.
  - `isControllable` is a computed property used to decide whether to show controls.
- `DeviceType(label, icon)`: defines supported device types and their UI labels/icon keys.
- `SwitchState`: ID, name, and status of one channel of a multi-switch panel.
- `LightSchedule`: on/off hours and minutes plus `enabled`.
  - `formattedOn()` and `formattedOff()` return `HH:mm` strings for the UI.
- `GridPosition`: a device's row and column within its room.
- `Room`: room ID/name and its location/span in the floor-plan grid.
- `Floor`: floor ID/name and its rooms.
- `SmartDevice`: complete stored device model: identity, placement, type, status, optional switch list, safety settings, schedule, camera URL, and last-on timestamp.
- `DeviceCreationRequest`: form data used to create `SmartDevice` records.
- `UsageLog`: one completed ON session, including start/end and duration in minutes.
- `SafetyAlert`: automatic safety notification generated when a critical device is forced off.
- `HomeState`: aggregate UI model containing the home name, floors, devices, logs, and alerts.

#### `data/DemoData.kt`

`DemoData` holds sample data for the in-memory repository.

- `initialHome` is a ready-made `HomeState` with sample floors, room grids, devices, logs, and a safety iron.
- `nowMinusHours(hours)` is a private timestamp helper used to make sample log history relative to the present time.

### 3.3 Repository contract and implementations

#### `data/repository/SmartHomeRepository.kt`

This interface is the boundary between UI/ViewModels and storage. A ViewModel should depend on this interface, not on Firebase directly.

Shared state streams:

- `homeState`: a `StateFlow<HomeState>` containing the latest full home snapshot.
- `alerts`: a `StateFlow<List<SafetyAlert>>` for active alerts.

Commands:

- `createFloor(name)`: creates a floor and returns its ID.
- `createRoom(...)`: adds a room to a floor-plan grid.
- `createDevice(request)`: creates a device and returns its ID.
- `toggleDevice(deviceId)`: turns a device on/off.
- `toggleSwitch(deviceId, switchId)`: toggles a single multi-switch channel.
- `updateMaxOnDuration(deviceId, minutes)`: changes iron safety limit.
- `updateLightSchedule(deviceId, schedule)`: saves schedule values.
- `refreshCameraSnapshot(deviceId)`: changes/refreshes camera snapshot state.
- `getDevice(deviceId)` and `getUsageForDevice(deviceId)`: immediate reads from the current cached state.
- `dismissAlert(alertId)`: acknowledges an alert.

#### `data/repository/FirebaseSmartHomeRepository.kt`

This is the production repository. It listens to Firebase and translates Firebase records into Kotlin models.

Important long-lived values:

- `homeReference`: Firebase reference to `homes/home1`.
- `scope`: background coroutine scope for automated monitors.
- `_homeState`: private mutable state.
- `homeState`: safe read-only view of `_homeState` for the rest of the app.
- `_alerts` / `alerts`: same private/public pair for alerts.

Public command implementations:

- `createFloor`, `createRoom`, and `createDevice` generate IDs and write the appropriate Firebase maps.
- `toggleDevice` reads the latest device and delegates to `turnDeviceOn` or `turnDeviceOff`.
- `toggleSwitch` changes one switch status, recalculates the multi-switch's overall status, and updates timing/logging where needed.
- `updateMaxOnDuration`, `updateLightSchedule`, and `refreshCameraSnapshot` write one device property.
- `getDevice` and `getUsageForDevice` read from the current `homeState` cache.
- `dismissAlert` sets the Firebase acknowledgement field.

Internal helpers:

- `turnDeviceOn(device)`: updates status and stores start time for timed devices.
- `turnDeviceOff(device, alertMessage)`: updates status, clears start time, writes a usage log, and optionally creates an alert.
- `addUsageLogUpdate(...)`: creates the multi-path Firebase update for a usage log.
- `updateHome(updates, action)`: submits an atomic multi-path update and reports a failure.
- `runSafetyMonitor()`: periodic coroutine checking all safety-critical ON devices.
- `runScheduleMonitor()`: periodic coroutine checking enabled light schedules.
- `isWithinSchedule(nowMinutes, schedule)`: supports both ordinary and overnight schedules.
- `toHomeState`, `toSmartDevice`, `toUsageLog`, and `toSafetyAlert`: convert Firebase `DataSnapshot` values to strongly typed Kotlin models.
- `string`, `int`, `long`, `boolean`: safe snapshot field readers with defaults.
- `emptyHomeState()`: initial state before Firebase returns home data.

#### `data/repository/DemoSmartHomeRepository.kt`

This alternative implementation has the same public functions as `FirebaseSmartHomeRepository`, but changes a local `MutableStateFlow` instead of Firebase.

- `scope`: background worker scope.
- `_homeState`/`homeState` and `_alerts`/`alerts`: local state streams.
- Creation, toggle, update, read, and dismissal methods follow the interface contract entirely in memory.
- `logUsage(...)`: adds an ON-session log after a device turns off.
- `runSafetyMonitor()` and `forceOffWithAlert(...)`: local implementation of iron safety logic.
- `runLightScheduleWorker()` and `isWithinSchedule(...)`: local automatic scheduling.

### 3.4 Android ViewModels

#### `viewmodel/HomeViewModel.kt`

`HomeViewModel` supplies the dashboard with lifecycle-aware state and commands.

- `homeState`: repository state made lifecycle-aware with `stateIn`.
- `alerts`: lifecycle-aware alert stream.
- `selectedFloorId`: current dashboard floor; only methods in the ViewModel can change it.
- `selectFloor(floorId)`: selects a floor locally.
- `createFloor(...)`: performs creation in `viewModelScope`, updates selection to the created floor, and invokes optional callback.
- `createRoom(...)` / `createDevice(...)`: execute repository commands in `viewModelScope`.
- `devicesForFloor(floorId)`: derived stream containing only devices on the requested floor.
- `floorById(floorId)`: current snapshot lookup.
- `toggleDevice(deviceId)` and `dismissAlert(alertId)`: repository delegation.
- `Factory.create(...)`: creates the ViewModel for Compose's `viewModel()` API.

#### `viewmodel/DeviceDetailViewModel.kt`

- `deviceId`: ID from the navigation route.
- `device`: derived stream that searches current home devices for that ID.
- `usageLogs`: derived stream of logs belonging to the device.
- `toggleDevice`, `toggleSwitch`, `updateMaxDuration`, `updateSchedule`, and `refreshCamera`: execute detail-screen actions in `viewModelScope`.
- `Factory.create(...)`: builds the ViewModel with repository and route device ID.

#### `viewmodel/UsageViewModel.kt`

- `DeviceUsageSummary`: report item containing device identity/type, today total, week total, and session count.
- `summaries`: a derived, descending-by-week-usage list of summaries.
- `allLogs`: logs sorted newest first.
- `buildSummaries(logs, devices)`: calculates 24-hour and 7-day totals for controllable/reportable device types.
- `Factory.create(...)`: ViewModel factory.

### 3.5 Android screens and reusable UI

#### `ui/screens/HomeScreen.kt`

`HomeScreen` is the dashboard.

- Collects `homeState` and `alerts` from `HomeViewModel`.
- Local state `selectedFloorId` controls the visible floor.
- Local state `isSetupOpen` controls the Add Floor/Room/Device dialog.
- The top plus button sets `isSetupOpen = true`.
- `Dialog` hosts `HomeManagementCard`; dismissing the dialog changes `isSetupOpen` to false.
- The floor chips call `viewModel.selectFloor`.
- `FloorPlanGrid` receives selected floor rooms and devices.
- `DeviceCard` opens details or toggles the selected device.
- `AlertBanner(message, onDismiss)` displays the newest alert and provides acknowledgement.

#### `ui/screens/HomeManagementCard.kt`

`HomeManagementCard` is the solid-background content placed inside the Home screen dialog.

- It maintains form-local values such as `mode`, names, selected floor/room IDs, grid coordinates, device type, safety settings, schedule, switch names, and camera URL.
- `mode` selects between Floor, Room, and Device forms.
- `roomOptions` is derived from the selected device floor.
- `LaunchedEffect(deviceFloorId, roomOptions)` ensures the selected device room remains valid when the floor changes.
- `FloorPicker`, `RoomPicker`, and `TypePicker` display wrapping chips to avoid narrow-screen overflow.
- `SmallNumberField` filters input to digits.
- `parseSchedule(start, end, enabled)` validates two `HH:mm` values and produces a `LightSchedule`, or returns null when invalid.
- The iron form places the duration field and `Safety critical` switch on separate responsive rows.

#### `ui/screens/DeviceDetailScreen.kt`

- `DeviceDetailScreen` collects the selected device and logs, shows basic status/control UI, then chooses the relevant device-specific section.
- Local `maxDuration` supports immediate slider feedback for irons.
- `MultiSwitchSection`: renders every switch and calls `toggleSwitch`.
- `IronSafetySection`: shows max ON duration and sends slider changes to `updateMaxDuration`.
- `LightScheduleSection`: displays schedule values and toggles enabled state through `updateSchedule`.
- `CameraSection`: displays the current snapshot with Coil and calls `refreshCamera`.

#### `ui/screens/UsageReportScreen.kt`

`UsageReportScreen` collects report summaries and logs from `UsageViewModel`, renders current per-device totals, and shows recent usage history. Its back control returns through navigation.

#### `ui/components/DeviceCard.kt`

- `DeviceCard(device, onClick, onToggle, modifier)` shows type icon, name, type, badge, and controllable switch.
- Whole-card click opens the detail page; the switch invokes `onToggle`.

#### `ui/components/FloorPlanGrid.kt`

- `FloorPlanGrid(rooms, devices, onDeviceClick, modifier)` calculates required rows/columns from room spans and draws the floor layout.
- `RoomCell(...)` displays room title, up to four device icons, and a `+N` indicator for additional devices.

#### `ui/components/DeviceIcon.kt`

- `deviceIcon(type)` maps every `DeviceType` to a Material icon vector.
- `DeviceTypeIcon(type)` renders the mapped icon.

#### `ui/components/StatusBadge.kt`

- `StatusBadge(status, modifier)` draws the textual status pill.
- `statusColor(status)` maps each status to a semantic color.

### 3.6 Android theme, resources, and build files

- `ui/theme/Color.kt`: global brand/status/surface color constants.
- `ui/theme/Type.kt`: global Material `Typography` value.
- `ui/theme/Theme.kt`: `DarkColorScheme`, `LightColorScheme`, and `SmarthomemonitoringappTheme()` which applies colors and typography.
- `AndroidManifest.xml`: registers Android app/activity metadata and permissions.
- `res/values/*.xml`: strings, colors, and Android theme resources.
- `res/xml/*.xml`: backup and data-extraction policy.
- `app/build.gradle.kts`, root `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`: Gradle plugins, dependencies, SDK versions, and project modules. They have build configuration, not runtime application functions.
- Template unit/instrumentation tests only verify the default Android test setup and do not contain domain logic.

## 4. Web simulator

### `src/firebase.ts`

- `database`: initialized Firebase Realtime Database client.
- `homeId`: Firebase home identifier. It uses `VITE_HOME_ID` when supplied; otherwise it uses `home1`.

### `src/types.ts`

Type-only file matching Firebase shape:

- `Status`, `DeviceType`: valid status/type unions.
- `Floor`, `Room`, `Switch`, `Schedule`, `Device`: persisted record shapes.
- `UsageLog`, `Alert`, `Home`: logs, alerts, and root home record.

### `src/main.tsx`

The React entry point. It mounts `<App />` into the HTML root element.

### `src/App.tsx`

#### Module-level helpers and globals

- `icons`: `DeviceType` to one-letter display mapping.
- `statusClass(status)`: status string to CSS class name.
- `timeLabel(timestamp)`: timestamp to localized display time.
- `scheduleTime(hour, minute)`: values to `HH:mm` string.
- `aggregateSwitchStatus(statuses)`: panel status prioritizing `ERROR`, then `DISCONNECTED`, then `ON`, otherwise `OFF`.
- `displayedStatus(device)`: returns aggregate multi-switch status or ordinary device status.
- `createId(prefix)`: creates a short UUID-based Firebase key.
- `parseTime(value)`: validates/parses browser time-input text.

#### `App()`

The simulator's root component.

State variables:

- `home`: latest Firebase home object; null while loading.
- `floor`: selected floor ID.
- `error`: Firebase connection/write error message.
- `pending`: identifier of the Firebase write currently in progress.

Effects and derived values:

- `useEffect(onValue(...))`: subscribes to `homes/{homeId}`, replaces `home` on every update, and selects the first floor if none is selected.
- `rooms`: memoized rooms whose `floorId` equals the selected floor.
- `devices`: memoized devices whose `floorId` equals the selected floor.

Firebase commands:

- `write(path, value, key)`: common async update function. It sets loading state, uses Firebase `update`, stores errors, and clears loading state.
- `toggle(id, device)`: toggles a device; for a multi-switch it changes all child switches; for irons/lights it also sets/clears `turnedOnAtMillis`.
- `createFloor(name)`: generates floor ID, writes record, then selects it.
- `createRoom(payload)`: generates ID and writes room placement data.
- `createDevice(payload)`: builds a type-appropriate device record, including optional schedule, safety limit, switches, or camera URL.
- `acknowledge(id)`: marks an alert acknowledged.

Child components:

- `HomeAdminPanel`: Floor/Room/Device creation form. Its local state contains all current form controls. Effects synchronize selected floors and selected rooms; `floorRooms` is the selected device floor's room list.
- `RoomCard`: displays one room and filters its devices.
- `DeviceCard`: displays/controls one device. `duration` measures current ON time; `updatePanelSwitch` changes one multi-switch channel.
- `IronLimitEditor`: stores an editable minute value and saves it to Firebase.
- `ScheduleEditor`: converts schedule data to browser time values and saves parsed times.
- `ActivityPanel`: sorts and shows the five newest alerts and logs.
- `ActivityCard`: reusable activity-panel wrapper.
- `Metric`: summary metric display.
- `State`: loading, empty, and error screen.

### `src/styles.css`

This is presentation-only CSS: page layout, cards, responsive layout, forms, button states, device status colors, and simulator visual design. It contains no application functions or runtime global variables.

### `src/vite-env.d.ts`

Vite TypeScript environment declaration. It exists only for type checking.

## 5. Responsibility boundaries

| Layer | Responsible for | Must not own |
| --- | --- | --- |
| Compose/React UI | Rendering, local form state, user events | Firebase data rules and device automation |
| ViewModel | Lifecycle-aware UI state and command dispatch | Widget layout and Firebase snapshot parsing |
| Repository | Database reads/writes, data conversion, automation | Screen navigation/layout |
| Models/types | Shared data shape | UI actions or Firebase calls |
| Firebase | Durable shared source of truth | Client-side presentation |

## 6. File-change guide

- Add a device field: update Android `DeviceModels.kt`, repository Firebase mapping/write code, simulator `types.ts`, and simulator creation/editor UI.
- Add a device type: update `DeviceType`, `DeviceIcon.kt`, form pickers, detail-screen `when` branch, Firebase conversion, and simulator `DeviceType`/UI conditionals.
- Change a Firebase field path: update both `FirebaseSmartHomeRepository.kt` and simulator `App.tsx`; the clients must use identical paths and field names.
- Change dashboard behavior: start in `HomeScreen.kt`, then follow callbacks to `HomeViewModel.kt` and `SmartHomeRepository.kt`.
- Change automated safety/schedule behavior: edit `FirebaseSmartHomeRepository.kt`; update `DemoSmartHomeRepository.kt` too if demo mode must remain equivalent.
