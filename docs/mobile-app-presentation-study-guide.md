# Mobile App Presentation Study Guide

This guide is a study document for presenting the Android `mobile-app` module. It explains what the app does, how the files and classes relate to each other, what the important methods do, how the workflows move through the code, and how errors and server connectivity are handled.

If you want one sentence to remember, it is this:

The app is a single-screen Android traveler client where `MainActivity` coordinates the screen, helper controllers handle focused features, API clients talk to the backend and weather service, and model classes carry the data.

## 1. What the app does

The traveler app lets a user:

- open a Google Map centered on the reserve area
- see reserve boundaries
- see public hazards on the map
- see public points of interest
- detect whether the traveler is currently inside a reserve
- view current weather and a short hourly forecast for the current location
- submit a traveler report with optional photo or video attachments

In presentation language:

This app is a field-facing mobile client for reserve visitors. It combines live location, reserve data, hazard visualization, weather information, and public reporting into one screen.

## 2. The big architecture

The design is intentionally simple:

- one main screen
- one main activity
- several focused helper classes
- direct HTTP calls with `HttpURLConnection`
- plain data model classes

There is no fragment navigation, no MVVM layer, and no Retrofit repository stack. The code favors direct, readable workflows.

### Architecture layers

1. Presentation layer
   `MainActivity`, `activity_main.xml`, `strings.xml`, buttons, panels, drawer, and weather overlay
2. Feature helper layer
   `MapController`, `LocationController`, `WeatherUiController`, `EventReportUiController`, `ReportMediaController`, `ReportSubmissionController`, `MapToggleUiController`, `EventPollingController`, `ReserveStateResolver`
3. Integration layer
   `ReserveApiClient`, `ReportApiClient`, `WeatherApiClient`
4. Data model layer
   `Reserve`, `AreaBounds`, `Event`, `Poi`, `ReserveState`, `TravelerReportData`, `WeatherCurrent`, `WeatherHourlyForecast`

### Relationship map

```text
MainActivity
|- binds activity_main.xml views
|- owns shared UI state and app state
|- uses MapController to draw the map
|- uses LocationController to get device location
|- uses ReserveStateResolver to compute reserve status
|- uses WeatherUiController for weather panel behavior
|- uses EventReportUiController for report panel and manual map location
|- uses ReportMediaController for attachments and camera capture
|- uses ReportSubmissionController to validate and upload reports
|- uses EventPollingController for hazard polling and backend retry
|- uses ReserveApiClient for reserves, hazards, and POIs
|- uses ReportApiClient indirectly through ReportSubmissionController
`- uses WeatherApiClient indirectly through WeatherUiController

ReserveApiClient -> backend /api/public/reserves, /events, /reserves/{id}/pois
ReportApiClient -> backend /api/public/reports
WeatherApiClient -> OpenWeather current weather and forecast endpoints
```

## 3. File map you should know

### Entry and configuration files

| File | Why it matters |
| --- | --- |
| `mobile-app/app/src/main/java/com/reserve/mobile/MainActivity.java` | Main entry point and screen coordinator |
| `mobile-app/app/src/main/AndroidManifest.xml` | Declares permissions, launcher activity, Maps API key, and `FileProvider` |
| `mobile-app/app/build.gradle` | Defines dependencies and `BuildConfig` values like backend and weather base URLs |
| `mobile-app/app/src/main/res/layout/activity_main.xml` | Defines the full screen layout |
| `mobile-app/app/src/main/res/values/strings.xml` | Contains user-facing text and many error/status messages |
| `mobile-app/app/src/main/res/xml/file_paths.xml` | Lets camera photos be shared through `FileProvider` |

### Java files grouped by role

| Group | Files |
| --- | --- |
| Screen coordination | `MainActivity` |
| Map and visual controls | `MapController`, `MapToggleUiController` |
| Location and reserve state | `LocationController`, `ReserveStateResolver`, `ReserveState` |
| Weather | `WeatherUiController`, `WeatherApiClient`, `WeatherCurrent`, `WeatherHourlyForecast` |
| Reporting | `EventReportUiController`, `ReportMediaController`, `ReportSubmissionController`, `ReportApiClient`, `TravelerReportData` |
| Backend reserve data | `ReserveApiClient`, `Reserve`, `AreaBounds`, `Event`, `Poi` |
| Background scheduling | `EventPollingController` |

## 4. Main screen walkthrough

The app uses one layout file, `activity_main.xml`, and the screen is organized into several visible sections.

### Map fragment

- The Google Map fills the background.
- This is where reserves, hazards, POIs, the my-location dot, and manual report marker appear.

### Top status card

- reserve name or location state
- reserve fire alert text
- hazard count summary
- general status text for loading, sending, failures, or waiting

This card is the app's status dashboard.

### Floating map controls

- menu button opens the drawer
- map layers button changes map type and traffic
- my location button recenters the map
- north-up button resets bearing and tilt
- weather toggle shows or hides the weather card

### Report button and report panel

- one button opens or hides the report form
- the form contains reserve selection, report type, description, name, media buttons, and submit button
- the traveler can either use live GPS or manually tap a map point

### Weather overlay

- shows compact current weather
- expands into a 6-item hourly forecast
- hidden while the report panel is open

### Side drawer

- shows backend server status
- shows POI and hazard toggle buttons

## 5. Core classes and how they relate

### `MainActivity`

#### Role

`MainActivity` is the app's central coordinator. It owns the main screen state and connects the controllers, API clients, and UI elements together.

#### State it owns

- loaded reserves
- loaded POIs
- loaded hazards
- current user location
- current reserve state
- whether the camera should follow the user
- whether weather is visible
- whether reserve, POI, or hazard refresh is already running
- the shared `ExecutorService`

#### Key methods you should know

| Method | What it does |
| --- | --- |
| `onCreate(...)` | Builds controllers, binds views, configures launchers and buttons, starts reserve loading |
| `bindViews()` | Connects XML view IDs to Java fields |
| `configureButtons()` | Wires button clicks to feature actions |
| `configureMap()` | Starts async map loading |
| `onMapReady(...)` | Attaches the `GoogleMap`, installs listeners, refreshes the map, starts location tracking |
| `loadReserves()` | Loads reserve boundaries from the backend |
| `loadPublishedHazards()` | Loads published hazards for every reserve |
| `loadReservePois()` | Loads public POIs for every reserve |
| `retryBackendConnection()` | Retry entry point used by the backend retry timer |
| `updateReserveState()` | Recomputes the current reserve state and redraws related UI |
| `toggleReportPanel()` | Opens or closes the report form |
| `startManualLocationSelection()` | Switches the next map tap into report-location selection |
| `toggleWeather()` | Shows or hides weather |
| `refreshMapContent()` | Redraws the map through `MapController` |
| `applyCurrentLocation(...)` | Saves latest location, optionally recenters, refreshes reserve state |
| `submitTravelerReport()` | Collects form state and delegates upload to `ReportSubmissionController` |
| `setBusyState(...)` | Disables or enables interactive controls during background work |
| `updateServerStatus(...)` | Updates the drawer badge to checking, online, or offline |
| `refreshWeather(...)` | Delegates weather UI updates to `WeatherUiController` |
| `onDestroy()` | Stops polling, stops tracking, shuts down executor |

#### Why `MainActivity` matters most

If you need to explain the whole app from one file, explain `MainActivity`. It is where startup happens, where helpers are created, where background loading begins, and where most flows meet again on the UI thread.

### Helper controllers

#### `MapController`

Role:
Draws reserve polygons, POI markers, and hazard markers on the `GoogleMap`.

Important methods:

- `attachMap(...)` stores the map and moves to the default camera
- `refresh(...)` clears and redraws the full map state
- `setShowPois(...)` turns POI markers on or off
- `setShowHazards(...)` turns hazard markers on or off
- `shouldRefreshAfterCameraIdle()` detects when icon size should change after zooming

Important details:

- highlights the active reserve with a stronger border
- uses custom icons for POI types
- uses a special fire icon for fire hazards
- changes icon size based on zoom level
- changes map style depending on whether POIs are shown

#### `MapToggleUiController`

Role:
Only handles the visual state of the toggle controls.

Important methods:

- `updateLabels(...)` updates POI, hazard, and weather toggle labels and colors

Why it exists:

It removes button-style code from `MainActivity` so the activity stays focused on workflow.

#### `LocationController`

Role:
Owns location permission handling and location updates.

Important methods:

- `startTracking(...)` requests permission if needed, enables the my-location layer, requests last known location, and starts live updates
- `onPermissionResult(...)` resumes or rejects tracking based on permission result
- `stopTracking()` stops location updates

Important details:

- uses `FusedLocationProviderClient`
- first tries the last known location for a fast initial result
- then requests continuous high-accuracy updates
- sends results back through the `Host` callback interface

#### `ReserveStateResolver`

Role:
Converts raw app data into a UI-facing reserve state.

Important methods:

- `resolve(...)` returns `NO_LOCATION`, `INSIDE_RESERVE`, or `OUTSIDE_RESERVE`

Important details:

- checks whether the current point lies inside a reserve's `AreaBounds`
- counts visible hazards
- checks whether the active reserve currently has a fire hazard

This class is important because it separates business logic from UI rendering.

#### `EventPollingController`

Role:
Repeats some action on a timer.

Important methods:

- `start()` begins periodic execution
- `stop()` cancels it

How it is used in this app:

- one instance polls hazards every 15 seconds
- one instance retries backend connection every 5 seconds after backend failures

#### `EventReportUiController`

Role:
Owns report panel visibility and manual map-point selection.

Important methods:

- `toggleReportPanel(...)` opens or closes the report panel
- `startManualLocationSelection(...)` changes the button so the next map tap becomes the report location
- `saveManualLocation(...)` stores the tapped point and creates or moves the marker
- `clearManualLocation(...)` removes manual state after successful submission or reset
- `updateReportLocationText(...)` decides whether to display manual location, GPS location, or waiting text

Important design choice:

Manual report location overrides live GPS for report submission.

#### `ReportMediaController`

Role:
Owns media attachment state.

Important methods:

- `openMediaPicker(...)` opens the Android document picker for images and videos
- `prepareCameraCapture()` creates a `FileProvider` URI for a photo
- `onCameraCaptureResult(...)` adds the new photo if capture succeeded
- `clearSelectedMedia()` clears the attachment list
- `getSelectedMediaUris()` exposes the chosen attachments

Important details:

- supports multiple attachments
- uses app external files storage
- shows the selected attachment count in the UI

#### `ReportSubmissionController`

Role:
Owns report validation, final location resolution, and upload orchestration.

Important methods:

- `submitReport(...)` validates the form and decides where the report location should come from
- `uploadReport(...)` builds `TravelerReportData` and sends it through `ReportApiClient`
- `handleMissingLocation(...)` restores UI state when a location could not be resolved

Important details:

- requires a selected reserve
- requires a non-empty description
- prefers manual location if one exists
- otherwise tries a fresh high-accuracy location
- updates busy UI state through its `Host`

This class is important because it keeps report rules out of `MainActivity`.

#### `WeatherUiController`

Role:
Owns weather UI state, caching, and loading behavior.

Important methods:

- `toggleExpanded()` expands or collapses hourly forecast
- `refreshWeather(...)` decides whether weather should be shown, loaded, reused, or hidden
- `shouldReloadWeather(...)` checks cache age and travel distance

Important details:

- current weather is cached
- hourly forecast only matters when expanded
- cache is reloaded after 30 minutes or after moving more than 1000 meters
- weather is hidden while the report panel is open

### API clients

#### `ReserveApiClient`

Role:
Loads reserves, hazards, and POIs from the backend public API.

Endpoints used:

- `GET {BACKEND_API_BASE}/reserves`
- `GET {BACKEND_API_BASE}/events?reserveId={id}`
- `GET {BACKEND_API_BASE}/reserves/{id}/pois`

Important methods:

- `loadReserves()`
- `loadPublishedHazards(...)`
- `loadPois(...)`
- parsing helpers like `parseReserve(...)`, `parseEvent(...)`, and `parsePoi(...)`

Important detail:

Hazards and POIs are loaded by iterating over every reserve and making one request per reserve.

#### `ReportApiClient`

Role:
Uploads traveler reports to the backend as multipart form data.

Endpoint used:

- `POST {BACKEND_API_BASE}/reports`

Important methods:

- `submitTravelerReport(...)`
- `writeFormField(...)`
- `writeFileField(...)`

Important detail:

Attachments are streamed from `Uri`s through `ContentResolver` and sent as repeated `attachments` parts.

#### `WeatherApiClient`

Role:
Calls OpenWeather and maps the JSON into weather model objects.

Endpoints used:

- current weather from `OPEN_WEATHER_API_BASE`
- forecast endpoint derived from the configured weather URL

Important methods:

- `hasApiKey()`
- `loadCurrentWeather(...)`
- `loadHourlyWeather(...)`

Important details:

- weather requests include latitude, longitude, metric units, and API key
- if the configured base ends in `/weather`, the client converts it to `/forecast` for hourly data
- weather condition text is normalized into readable capitalized words

### Models

#### `Reserve`

- stores reserve ID, display name, center coordinates, and `AreaBounds`
- `toString()` returns the display name so the spinner shows readable text

#### `AreaBounds`

- stores min and max latitude and longitude
- `contains(...)` is the core geofencing check

#### `Event`

- stores reserve ID, type, priority, description, and coordinates
- `isFire()` helps trigger the fire alert UI
- `latLng()` converts the model into a map point

#### `Poi`

- stores reserve ID, type, name, description, and coordinates
- normalizes missing names and types

#### `ReserveState`

- compact UI state object
- represents no location, inside reserve, or outside reserve
- also stores hazard count and whether the active reserve has a fire hazard

#### `TravelerReportData`

- the report payload sent to the backend
- includes reserve ID, type, description, location, reporter name, and attachment URIs

#### `WeatherCurrent` and `WeatherHourlyForecast`

- simple weather data holders
- used by `WeatherUiController` to render text

## 6. End-to-end workflows

### Startup workflow

1. Android launches `MainActivity`.
2. `onCreate(...)` sets the layout.
3. `MainActivity` creates:
   - API clients
   - controllers
   - a single-thread `ExecutorService`
4. Views are bound from `activity_main.xml`.
5. Activity result launchers are registered for:
   - media picking
   - camera capture
   - location permissions
6. Button listeners are attached.
7. The map fragment is asked to load asynchronously.
8. The drawer server status is set to checking.
9. Reserve loading starts immediately.

This means the app does not wait for the map before starting backend work.

### Map startup workflow

1. Google Maps finishes initialization.
2. `onMapReady(...)` receives the `GoogleMap`.
3. Default controls are customized.
4. Camera listeners and map-tap listener are attached.
5. `MapController.attachMap(...)` stores the map and moves the camera to the default area.
6. `refreshMapContent()` draws whatever data is already available.
7. `startLocationTracking()` begins location flow.

### Reserve and hazard workflow

#### Reserve loading

1. `loadReserves()` runs on the executor.
2. `ReserveApiClient.loadReserves()` calls `/reserves`.
3. JSON is parsed into `Reserve` objects.
4. Back on the UI thread, `onReservesLoaded(...)`:
   - stores the list
   - fills the reserve spinner
   - refreshes the map
   - refreshes reserve state
   - starts hazard loading
   - starts POI loading

#### Hazard loading

1. `loadPublishedHazards()` checks that reserves exist and a refresh is not already running.
2. For each reserve, `ReserveApiClient` calls `/events?reserveId={id}`.
3. All results are combined into one `allHazards` list.
4. `onHazardsLoaded(...)`:
   - stores the hazards
   - redraws the map
   - updates reserve state
   - clears the status line
   - marks backend online
   - stops backend retry
   - starts hazard polling

#### POI loading

1. `loadReservePois()` checks state flags.
2. For each reserve, `ReserveApiClient` calls `/reserves/{id}/pois`.
3. All POIs are stored in `allPois`.
4. The map refreshes.

### Location and reserve detection workflow

1. `LocationController.startTracking(...)` checks permission.
2. If permission is missing, it asks the activity to request it.
3. If permission exists:
   - the Google Maps my-location layer is enabled
   - last known location is requested
   - continuous updates are subscribed
4. When a location arrives, `MainActivity.applyCurrentLocation(...)` runs.
5. The activity stores `currentUserLatLng`.
6. `updateReserveState()` asks `ReserveStateResolver.resolve(...)` for the new state.
7. The activity updates:
   - location hint text
   - spinner selection
   - fire alert UI
   - hazard count text
   - report location text
   - map highlight
   - weather refresh

#### Reserve matching logic

The matching rule is simple:

- every reserve has rectangular `AreaBounds`
- if the current latitude and longitude fall inside those bounds, the user is considered inside that reserve

This is easy to explain in a presentation because it is readable and deterministic.

### Weather workflow

1. The traveler taps the weather toggle.
2. `MainActivity.toggleWeather()` flips `showWeather`.
3. `refreshWeather(...)` delegates to `WeatherUiController`.
4. `WeatherUiController` checks:
   - should the panel be visible
   - is location available
   - is the weather API key configured
   - is cached weather still valid
5. If cached weather is good, it renders immediately.
6. If not, it loads current weather.
7. If the panel is expanded, it also loads hourly forecast.
8. Results are rendered back on the UI thread.

#### Weather cache rules

- reload if weather was never loaded
- reload if forced by the activity
- reload if cached data is older than 30 minutes
- reload if the user moved more than 1000 meters

#### Good presentation point

The weather feature is optimized to avoid unnecessary network calls by using both time-based and distance-based cache invalidation.

### Report workflow

1. The traveler opens the report panel.
2. The traveler selects:
   - reserve
   - report type
   - optional name
   - description
3. The traveler can:
   - attach media from the picker
   - capture a photo with the camera
   - use current GPS location
   - tap the map to choose a manual location
4. `submitTravelerReport()` passes everything to `ReportSubmissionController`.
5. `ReportSubmissionController.submitReport(...)` validates input.
6. Location is resolved:
   - manual map point first
   - otherwise fresh high-accuracy GPS
7. `TravelerReportData` is created.
8. `ReportApiClient.submitTravelerReport(...)` sends a multipart HTTP request.
9. On success:
   - server status becomes online
   - draft form resets
   - report panel closes
   - success status text and toast appear
   - hazards reload
10. On failure:
   - server status becomes offline
   - busy state ends
   - failure status text appears

#### Why hazards reload after submit

This gives the traveler an updated view in case the backend now includes the newly submitted report in the public hazard feed.

## 7. Error handling and resilience

This app uses direct, visible error handling rather than complex background recovery logic.

### Backend connection handling

#### Where the backend URL comes from

In `mobile-app/app/build.gradle`, the app defines:

- `BACKEND_API_BASE`
- default value: `http://10.0.2.2:8080/api/public`

`10.0.2.2` is the Android emulator alias for the host machine's localhost. That is how the emulator reaches a backend running on the development computer.

#### Public backend endpoints used by the app

The mobile client talks to the backend public controller:

- `GET /api/public/reserves`
- `GET /api/public/events?reserveId=...`
- `GET /api/public/reserves/{reserveId}/pois`
- `POST /api/public/reports`

These match the backend class:

- `backend/src/main/java/com/noam/fleetcommand/events/PublicTravelerController.java`

#### How network calls are made

The app uses `HttpURLConnection` directly.

The common pattern is:

1. build the URL from `BuildConfig`
2. open the connection
3. set request method and headers
4. read the response
5. require a 2xx status code
6. throw an exception otherwise

#### How server status is shown to the user

The drawer has a server status label:

- checking
- online
- offline

`MainActivity.updateServerStatus(...)` controls that label.

#### Automatic backend retry

The app has a dedicated backend retry controller:

- interval: 5 seconds
- action: `retryBackendConnection()`

When reserve loading fails:

- status text changes to reserve-load-failed
- server status becomes offline
- retry starts

When hazard loading fails:

- status text changes to hazard-load-failed
- server status becomes offline
- retry starts

What retry does:

- if reserves are still empty, retry reserve loading
- otherwise retry hazard loading
- if POIs are still empty, retry POI loading too

When a later hazard refresh succeeds:

- server status becomes online
- retry stops

This is one of the strongest presentation points in the app because it shows resilience when the backend is temporarily unavailable.

#### Continuous hazard polling

The app also has a second timer:

- interval: 15 seconds
- action: `loadPublishedHazards()`

This keeps traveler-visible hazards updated after startup without requiring manual refresh.

#### Request overlap protection

`MainActivity` uses flags to avoid duplicate simultaneous loads:

- `reserveRefreshInFlight`
- `poiRefreshInFlight`
- `hazardRefreshInFlight`

This matters because polling and retry are timer-based. The flags make sure the app does not stack duplicate network calls.

### User-facing error handling cases

#### 1. Location permission denied

Handled by:

- `LocationController`
- `MainActivity.locationControllerHost.onLocationPermissionDenied()`

Result:

- status text explains that location permission is needed
- reserve state is recomputed as no location
- map location-dependent features wait

#### 2. No last-known location or current GPS fix

Handled by:

- `LocationController.requestLastKnownLocation(...)`
- `ReportSubmissionController.handleMissingLocation(...)`

Result:

- UI says location is waiting
- report flow tells the user to wait for GPS or pick a point on the map

#### 3. Reserve load fails

Handled by:

- `MainActivity.loadReserves()`

Result:

- server status becomes offline
- top status says reserve boundaries could not be loaded
- automatic backend retry begins

#### 4. Hazard refresh fails

Handled by:

- `MainActivity.loadPublishedHazards()`

Result:

- server status becomes offline
- top status says hazards could not be refreshed
- automatic backend retry begins

#### 5. POI load fails

Handled by:

- `MainActivity.loadReservePois()`

Result:

- refresh flag is cleared
- short toast says POIs could not load
- app keeps working

Important nuance:

POI failure is treated as non-fatal compared to reserve and hazard failure.

#### 6. Report upload fails

Handled by:

- `ReportSubmissionController.uploadReport(...)`

Result:

- server status becomes offline
- busy state ends
- top status says the report could not be sent

There is no automatic retry for report submission. The user can submit again.

#### 7. Camera preparation or capture fails

Handled by:

- `MainActivity.launchCameraCapture()`
- `ReportMediaController.onCameraCaptureResult(...)`

Result:

- toast says camera photo could not be prepared or was not saved

#### 8. Weather API key missing

Handled by:

- `WeatherUiController.refreshWeather(...)`
- `WeatherApiClient.hasApiKey()`

Result:

- weather panel shows a direct message saying the key is missing

#### 9. Weather request fails

Handled by:

- `WeatherUiController.refreshWeather(...)`

Result:

- weather area says weather is unavailable
- backend server status is not affected

This is correct because weather is a separate service from the backend.

### Data parsing safety

The API clients use many `opt...` JSON calls and fallback values.

Examples:

- unknown event type defaults to `OTHER`
- unknown priority defaults to `LOW`
- missing coordinates become `Double.NaN`
- missing reserve names fall back to readable defaults

This means the app is defensive against partial or imperfect JSON responses.

## 8. Threading and async model

This app does not create many custom threads. It uses a simple async pattern:

- one single-thread `ExecutorService` for background work
- Android callbacks for activity results and location
- `Handler` inside `EventPollingController` for timers
- `runOnUiThread(...)` to update views after background work

### Why a single-thread executor helps

- simple to reason about
- avoids many race conditions
- keeps network tasks serialized

### Important async examples

- reserve loading runs on the executor
- hazard loading runs on the executor
- POI loading runs on the executor
- report upload runs on the executor
- weather loading runs on the executor
- GPS callbacks return on Android callback paths

## 9. How to explain the app in a presentation

Here is a strong simple story:

1. The app opens into one map-centered screen.
2. `MainActivity` immediately starts loading reserve data and preparing the map.
3. Once location is available, the app determines whether the traveler is inside a reserve.
4. It then overlays reserve boundaries, hazards, and POIs on Google Maps.
5. The weather feature uses the current location and smart caching.
6. The report feature lets the traveler submit a typed report with optional media and either GPS or manual map coordinates.
7. The app keeps hazard data fresh by polling and can recover automatically if the backend starts late or temporarily goes down.

That explanation covers the app's main technical value without getting lost in small implementation details.

## 10. Questions you should be ready to answer

### Why did you use one activity?

Because the app is one main traveler screen, so one activity keeps the user flow simple and the code easy to follow.

### Why did you create helper controllers?

To keep `MainActivity` from becoming even larger and to isolate focused responsibilities like weather, map rendering, report submission, and location tracking.

### How does reserve detection work?

The current GPS location is checked against each reserve's rectangular `AreaBounds`. If the point falls inside one rectangle, the traveler is considered inside that reserve.

### How do you avoid duplicate network calls?

The app uses in-flight flags for reserve, POI, and hazard loading, plus one single-thread executor.

### How does the app handle backend downtime?

It marks the server as offline, shows a failure message, and starts a retry timer that checks again every 5 seconds.

### How do reports include media?

The app collects `Uri`s from the picker or camera, then `ReportApiClient` streams them into a multipart HTTP POST request.

### How is weather optimized?

The app caches weather and only reloads after enough time has passed, the user moved far enough, or the user explicitly forces a refresh.

## 11. Short study checklist

Before your presentation, make sure you can explain these points without looking at the code:

- what `MainActivity` owns
- why helper controllers exist
- which classes talk to the backend
- which backend endpoints are used
- how reserve matching works
- how hazard polling works
- how backend retry works
- how report submission chooses a location
- how camera and media attachments are handled
- how weather caching works
- how offline and failure states are shown in the UI

## 12. Best order to revise the code before presenting

If you want a fast re-study session, read the code in this order:

1. `mobile-app/README.md`
2. `mobile-app/app/build.gradle`
3. `mobile-app/app/src/main/AndroidManifest.xml`
4. `mobile-app/app/src/main/res/layout/activity_main.xml`
5. `mobile-app/app/src/main/java/com/reserve/mobile/MainActivity.java`
6. `MapController.java`
7. `LocationController.java`
8. `ReserveStateResolver.java`
9. `WeatherUiController.java`
10. `ReportSubmissionController.java`
11. `ReserveApiClient.java`
12. `ReportApiClient.java`
13. `WeatherApiClient.java`
14. the model classes

## 13. Final mental model

Remember this:

- `MainActivity` coordinates everything
- controllers own focused workflows
- API clients perform network communication
- model classes hold the data
- the map is the center of the UI
- reserve state, weather, and report behavior all depend on location
- the app is resilient because it polls hazards and retries backend connection automatically

That is the clearest way to understand and present the app.
