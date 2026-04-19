# MainActivity Workflow

This document explains the runtime workflow of `MainActivity` in `mobile-app/app/src/main/java/com/reserve/mobile/MainActivity.java`.

`MainActivity` is the main coordinator of the Android app. It does not do every task itself, but it owns the overall screen state and decides when helper controllers and API client classes should run.

## Main Responsibilities

- create and wire together the main screen
- connect buttons, spinners, the map, and the report panel
- request and react to user location updates
- load reserves and hazards from the backend
- delegate weather loading to `WeatherUiController`
- collect report form data and send traveler reports
- keep the map, reserve-state UI, and status text in sync

## Main Collaborators

- `MapController`
  Draws reserves and hazards on the Google Map.
- `WeatherUiController`
  Manages weather loading, caching, and weather UI updates.
- `EventPollingController`
  Triggers repeated hazard refreshes every few seconds.
- `EventReportUiController`
  Manages report panel UI state and manual report location selection.
- `LocationController`
  Manages permission-result handling, last-known location lookup, and live location updates.
- `ReserveStateResolver`
  Computes `ReserveState` from the current location, reserve list, and hazard list.
- `ReportMediaController`
  Manages report attachments, camera capture state, and selected-media UI text.
- `ReportSubmissionController`
  Validates report inputs, resolves report coordinates, and uploads reports.
- `ReportApiClient`
  Uploads traveler reports and attachments to the backend.
- `MapToggleUiController`
  Updates the visual state of the layer toggle buttons.
- `ReserveApiClient`
  Loads reserves and published hazards.
- `WeatherApiClient`
  Loads current weather and hourly forecast data.

## Startup Flow

When the app screen opens, `onCreate()` runs first.

The startup sequence is:

1. `setContentView(...)` inflates `activity_main.xml`.
2. Core helpers are available or created for runtime use:
   `FusedLocationProviderClient`, `ReserveStateResolver`, `MapToggleUiController`, `MapController`, `WeatherUiController`, `EventPollingController`, `EventReportUiController`, `LocationController`, and `ReportSubmissionController`.
3. `bindViews()` caches references to all important UI elements.
4. `ReportMediaController` is created after the relevant views are bound.
5. Setup methods register UI behavior:
   `configureTypeSpinner()`, `configureMediaPicker()`, `configureLocationPermissionLauncher()`, `configureButtons()`, and `configureMap()`.
6. Initial UI state is prepared:
   toggle labels, server status text, selected media text, and report location text.
7. A loading status is shown.
8. `loadReserves()` starts the first backend fetch.

At this point, the activity is visible, but the map and location flow may still be initializing asynchronously.

## Map Initialization Flow

`configureMap()` gets the `SupportMapFragment` and requests `getMapAsync(this)`.

Later, Android calls `onMapReady(GoogleMap map)`.

Inside `onMapReady()`:

1. The `GoogleMap` instance is stored.
2. Default map UI elements that the app does not want are disabled.
3. Camera movement is observed so the app can stop auto-following if the user manually drags the map.
4. Map taps are routed to `handleMapClickForManualLocation(...)`.
5. `mapController.attachMap(map)` connects the helper controller to the map.
6. `refreshMapContent()` draws the current reserve and hazard data.
7. `startLocationTracking()` begins the location flow.
8. The temporary status text is cleared.

## Reserve and Hazard Data Flow

### Reserve loading

`loadReserves()` uses the shared `ExecutorService` to do backend work off the UI thread.

The sequence is:

1. `ReserveApiClient.loadReserves()` is called in the background.
2. On success, `runOnUiThread(...)` calls `onReservesLoaded(...)`.
3. `onReservesLoaded(...)`:
   updates server status to online, replaces the reserve list, fills the reserve spinner, refreshes the map, refreshes the reserve state UI, sets a "loading hazards" status, and starts the first hazard fetch.
4. On failure, the activity marks the server as offline and shows an error status.

### Hazard loading

`loadPublishedHazards()` is the main hazard refresh method.

It first refuses to run if:

- reserves are not loaded yet
- a previous hazard refresh is still in flight

If it can run:

1. `hazardRefreshInFlight` is set to `true`.
2. `ReserveApiClient.loadPublishedHazards(reserves)` runs on the executor.
3. On success, `onHazardsLoaded(...)` runs on the UI thread.
4. `onHazardsLoaded(...)`:
   updates server status, replaces the hazard list, refreshes the map, refreshes the reserve state UI, clears the status text, resets `hazardRefreshInFlight`, and starts polling.
5. On failure, the activity marks the server as offline, shows an error status, and resets `hazardRefreshInFlight`.

### Hazard polling

After the first successful hazard load, `startHazardPolling()` starts `EventPollingController`.

That controller calls `loadPublishedHazards()` every `15` seconds.

This keeps the map fresh without reloading the whole activity.

## Location Flow

`MainActivity` starts location tracking from `onMapReady()`.

The location sequence is:

1. `startLocationTracking()` delegates to `LocationController`.
2. `LocationController` checks location permission.
3. If permission is missing, it launches the permission request flow.
4. If permission exists, it enables the map's my-location layer.
5. It asks for the last known location first, to get fast initial UI feedback.
6. It also subscribes to continuous location updates from `FusedLocationProviderClient`.

Whenever a location update arrives:

1. `currentUserLatLng` is updated.
2. If the map is still following the user, `moveCameraToUser(...)` recenters the map.
3. `updateReserveState()` recalculates the current reserve state and refreshes related UI.

## Reserve State Flow

`updateReserveState()` is the center of the "where is the user now?" logic.

It does the following:

1. Calls `ReserveStateResolver` to compute a `ReserveState`.
2. Stores the result in `currentReserveState`.
3. Chooses one of three UI states:
   no location yet, inside a reserve, or outside all reserves.
4. Updates the hazard count text, based on whether the hazard layer is enabled.
5. Calls `finalizeReserveStateRefresh()`.

`finalizeReserveStateRefresh()` then performs the shared follow-up work:

- updates the report location text
- refreshes map content
- refreshes weather

So one location change can affect multiple visible parts of the screen at once.

## Layer Toggle Flow

The activity exposes three map-related controls:

- POI toggle
- hazard toggle
- weather toggle

The POI and hazard buttons update state inside `MapController`, then call `onMapLayerChanged(...)`.

`onMapLayerChanged(...)`:

1. redraws the map
2. refreshes toggle labels
3. optionally refreshes reserve-state UI such as hazard-count text

The weather button flips `showWeather`, updates button labels, and calls `refreshWeather(...)`.

The weather expand button tells `WeatherUiController` to expand or collapse the hourly forecast area.

## Weather Flow

`MainActivity` does not load weather data directly. It delegates that part to `WeatherUiController`.

The activity calls `refreshWeather(...)` in these situations:

- after the weather toggle changes
- after reserve-state refreshes
- after the weather expand button is pressed
- after the app receives initial or updated location data

The activity passes:

- whether weather is enabled
- whether the refresh should be forced
- the current user location
- the weather-related views that should be updated

This keeps weather logic mostly outside `MainActivity`, while `MainActivity` still decides when weather should react.

## Report Submission Flow

`MainActivity` owns the report screen state, while `ReportSubmissionController` owns the actual submission workflow.

### Preparing the report UI

- `toggleReportPanel()` opens or closes the report panel.
- `configureTypeSpinner()` sets the report type list.
- `configureMediaPicker()` registers attachment and camera callbacks that delegate to `ReportMediaController`.
- `startManualLocationSelection()` enables map-tap mode for manual report coordinates.
- `handleMapClickForManualLocation(...)` saves the tapped location through `EventReportUiController`.

### Submitting a report

When the user presses submit, `submitTravelerReport()` runs.

The sequence is:

1. `MainActivity` gathers the current form values and selected media from `ReportMediaController`, then passes them to `ReportSubmissionController`.
2. `ReportSubmissionController` validates that a reserve is selected and description text exists.
3. It checks whether the user already picked a manual report location.
4. If manual location exists:
   it reuses that point and asks `MainActivity` to refresh current location state.
5. If manual location does not exist:
   it makes sure location permission exists.
6. It requests a fresh high-accuracy phone location using `getCurrentLocation(...)`.
7. On success, it updates `MainActivity` with the resolved coordinates and continues to upload.
8. On failure, it restores UI state and shows a location-related error.

### Uploading a report

`ReportSubmissionController` performs the backend submission on the executor.

It:

1. builds a `TravelerReportData` object from the form fields, current coordinates, and selected media
2. calls `ReportApiClient.submitTravelerReport(...)`
3. on success:
   marks the server online, triggers `MainActivity` cleanup through host callbacks, restores enabled UI state, shows success feedback, and reloads hazards
4. on failure:
   marks the server offline and shows a failure status

## Threading Model Inside MainActivity

`MainActivity` uses three thread-related patterns:

- main thread:
  all view updates and normal Android callback handling
- `ExecutorService`:
  reserve loading, hazard loading, and report upload
- `runOnUiThread(...)`:
  returns background results back to the UI safely

So a common pattern in this class is:

1. start work on the executor
2. wait for the API client call to finish
3. switch back to the UI thread
4. update views and in-memory state

## Shutdown Flow

When the activity is destroyed, `onDestroy()` runs.

It:

1. stops hazard polling
2. asks `LocationController` to stop tracking
3. shuts down the executor

This prevents background work and location listeners from leaking after the screen is closed.

## Short Mental Model

The easiest way to remember `MainActivity` is:

- startup wires the screen and begins reserve loading
- map readiness begins location tracking
- location drives reserve state and weather refresh
- reserve loading enables hazard loading and then hazard polling
- report submission gathers UI data and sends it through `ReportApiClient`

So `MainActivity` is less a "single feature" class and more the traffic controller of the whole mobile screen.
