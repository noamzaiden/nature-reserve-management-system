# Mobile App Functionality Description

## Scope
This document describes the current implementation inside `mobile-app/` based on the source files present in the repository on April 18, 2026. It focuses on real behavior, class relationships, XML/JSON usage, and the purpose of the major folders.

## High-level summary
`mobile-app` is a single-activity Android app for travelers in or near nature reserves. The current app does four main things:

- Shows a Google Map and follows the device location.
- Detects whether the user is inside one of the backend-defined reserve rectangles.
- Downloads and displays published hazards on the map.
- Lets the user send a traveler report with text and optional media attachments.

Weather is an optional overlay. Reserve and hazard data come from the backend. Weather comes from OpenWeather.

## Folder structure

| Path | Purpose |
| --- | --- |
| `mobile-app/README.md` | Developer-facing run instructions, emulator/backend assumptions, and required API keys. |
| `mobile-app/build.gradle` | Root Gradle plugin declaration. |
| `mobile-app/settings.gradle` | Gradle module/repository setup. Includes only `:app`. |
| `mobile-app/gradle.properties.example` | Example local secrets file for `MAPS_API_KEY` and `OPEN_WEATHER_API_KEY`. |
| `mobile-app/gradlew`, `mobile-app/gradlew.bat` | Gradle wrapper launch scripts. |
| `mobile-app/gradle/wrapper/` | Standard Gradle wrapper binary and version metadata. Not application logic. |
| `mobile-app/app/` | The actual Android application module. |
| `mobile-app/app/build.gradle` | App module configuration: SDK levels, Java version, dependencies, and `BuildConfig` API constants. |
| `mobile-app/app/src/main/AndroidManifest.xml` | Android permissions, app metadata, `FileProvider`, and `MainActivity` launcher registration. |
| `mobile-app/app/src/main/java/com/reserve/mobile/` | All Java application logic. |
| `mobile-app/app/src/main/res/` | Android resources: layouts, strings, dimensions, drawables, raw JSON, and XML config. |

## Runtime architecture

Visual diagram: [mobile-app-relations.drawio](mobile-app-relations.drawio)

The app is organized around one screen and several helper classes:

```text
MainActivity
|- MapController -> renders reserve polygons, hazards, and map styling
|- WeatherUiController -> weather UI state/caching
|  \- WeatherRepository -> OpenWeather HTTP + JSON parsing
|- EventPollingController -> periodic hazard refresh trigger
|- EventReportUiController -> report panel state and manual map pin
|- MapToggleUiController -> toggle button labels/styles
|- ReserveNetworkRepository -> backend HTTP + JSON parsing + multipart report upload
|  \- HttpUtils -> shared GET/response helpers
|- ReserveUtils -> reserve lookup and hazard counting
`- Data models: Reserve, AreaBounds, Event, TravelerReportData,
   WeatherCurrent, WeatherHourlyForecast
```

The architecture is simple and direct:

- `MainActivity` owns almost all screen state.
- Helper/controller classes exist mainly to keep `MainActivity` from becoming even larger.
- There is no MVVM layer, no Room database, no dependency injection, and no Fragments.
- Networking runs through a single-thread `ExecutorService`, so backend/weather work is serialized.

## Text relation diagram

```text
External Systems
├─ Android / Google Play Services
│  ├─ AppCompatActivity
│  ├─ SupportMapFragment / GoogleMap
│  ├─ FusedLocationProviderClient
│  └─ ActivityResult APIs
├─ Backend API
│  ├─ GET /reserves
│  ├─ GET /events?reserveId=...
│  └─ POST /reports
└─ OpenWeather API
   ├─ GET /weather
   └─ GET /forecast

MainActivity
├─ uses MapController
│  ├─ reads Reserve
│  ├─ reads AreaBounds
│  └─ reads Event
├─ uses WeatherUiController
│  ├─ uses WeatherRepository
│  │  ├─ uses ApiConfig
│  │  ├─ uses HttpUtils
│  │  ├─ creates WeatherCurrent
│  │  └─ creates WeatherHourlyForecast
│  ├─ stores WeatherCurrent
│  └─ stores List<WeatherHourlyForecast>
├─ uses EventPollingController
│  └─ triggers loadPublishedHazards()
├─ uses EventReportUiController
│  └─ manages report panel + manual map location
├─ uses MapToggleUiController
│  └─ reads MapController state
├─ uses ReserveNetworkRepository
│  ├─ uses ApiConfig
│  ├─ uses HttpUtils
│  ├─ creates Reserve
│  ├─ creates AreaBounds
│  ├─ creates Event
│  └─ consumes TravelerReportData
├─ uses ReserveUtils
│  ├─ finds current Reserve from LatLng
│  └─ counts visible Event objects
└─ creates TravelerReportData

Domain / Data Objects
├─ Reserve
│  └─ has AreaBounds
├─ Event
│  └─ belongs to reserveId
├─ TravelerReportData
│  └─ holds report fields + attachment URIs
├─ WeatherCurrent
└─ WeatherHourlyForecast
```

## Main runtime flow

1. `MainActivity.onCreate()` inflates `activity_main.xml`, creates helpers, binds views, configures launchers/buttons, and starts loading reserves.
2. `SupportMapFragment` asynchronously provides the `GoogleMap` in `onMapReady()`.
3. `MapController.attachMap()` applies the initial map style and camera.
4. `MainActivity.startLocationTracking()` requests location permission if needed, then starts fused location updates.
5. `ReserveNetworkRepository.loadReserves()` downloads reserve data from the backend.
6. After reserves load, `ReserveNetworkRepository.loadPublishedHazards()` loads hazards for each reserve and populates the map.
7. `EventPollingController` refreshes hazards every 15 seconds once reserves exist.
8. Each location update runs `ReserveUtils.findReserveForLocation()` to determine whether the user is inside a reserve and updates the summary text, spinner selection, map highlight, and weather overlay.
9. When the user submits a report, `MainActivity` builds `TravelerReportData` and calls `ReserveNetworkRepository.submitTravelerReport()` to upload a multipart form with optional attachments.

## Class-by-class functionality and relationships

### Activity and screen orchestration

- `MainActivity`
  Responsibility: the entry point, main screen controller, and central coordinator.
  Real behavior:
  - Owns the Google Map lifecycle, fused location tracking, and all visible UI state.
  - Loads reserves and hazards from the backend.
  - Delegates map drawing, weather handling, toggle styling, hazard polling, and report-panel state to helper classes.
  - Builds and uploads traveler reports.
  Direct relationships:
  - Uses `ReserveNetworkRepository` for reserve/hazard loading and report submission.
  - Uses `WeatherRepository` indirectly through `WeatherUiController`.
  - Uses `MapController` to redraw the map.
  - Uses `ReserveUtils` to detect the current reserve and count visible hazards.
  - Uses `EventPollingController`, `EventReportUiController`, and `MapToggleUiController`.
  - Produces `TravelerReportData`.

### UI and workflow helpers

- `MapController`
  Responsibility: owns map rendering behavior.
  Real behavior:
  - Attaches to `GoogleMap`.
  - Clears and redraws reserve boundary polygons.
  - Highlights the current reserve with different fill/stroke colors.
  - Draws hazard markers with color based on hazard priority.
  - Applies one of two raw JSON map styles depending on the POI toggle.
  Direct relationships:
  - Consumes `Reserve`, `AreaBounds`, and `Event`.
  - Loads `R.raw.map_style_nature` or `R.raw.map_style_nature_no_poi`.
  Important note:
  - Despite the "POI" naming, the helper does not draw reserve center markers. The toggle only switches the Google Map style JSON.

- `WeatherUiController`
  Responsibility: weather overlay state, cache policy, and rendering.
  Real behavior:
  - Shows/hides the weather card.
  - Expands/collapses the hourly forecast panel.
  - Caches weather by time and distance.
  - Loads current weather and forecast on the background executor.
  Direct relationships:
  - Uses `WeatherRepository` for API calls.
  - Stores `WeatherCurrent` and `List<WeatherHourlyForecast>`.
  - Is called by `MainActivity`.

- `EventPollingController`
  Responsibility: periodic polling loop.
  Real behavior:
  - Uses a main-thread `Handler` to schedule repeated hazard refreshes.
  - Calls back into `MainActivity.loadPublishedHazards()`.
  Direct relationships:
  - Receives a `BooleanSupplier` and `Runnable` from `MainActivity`.

- `EventReportUiController`
  Responsibility: UI state for the report panel and manual location selection.
  Real behavior:
  - Shows/hides the report panel.
  - Tracks whether the next map tap should set report coordinates.
  - Creates/removes a manual report marker on the map.
  - Decides whether the displayed report location comes from the manual pin or live GPS.
  Direct relationships:
  - Called by `MainActivity`.
  - Uses `GoogleMap`, `Marker`, and `LatLng`.

- `MapToggleUiController`
  Responsibility: visual state of the POI, hazard, and weather toggles.
  Real behavior:
  - Updates labels such as `POI on/off` and `Hazards on/off`.
  - Applies different fills/strokes to the side-menu buttons.
  - Switches weather button icon tint and background state.
  Direct relationships:
  - Reads state from `MapController`.
  - Uses drawable resources for the weather button background.

### Data access and network helpers

- `ReserveNetworkRepository`
  Responsibility: backend communication for reserve and hazard data, plus report upload.
  Real behavior:
  - `loadReserves()` GETs `/reserves`.
  - `loadPublishedHazards()` loops over every reserve and GETs `/events?reserveId={id}`.
  - `submitTravelerReport()` POSTs multipart form data to `/reports`.
  Direct relationships:
  - Uses `ApiConfig.BACKEND_API_BASE`.
  - Uses `HttpUtils` for GET requests.
  - Creates `Reserve`, `AreaBounds`, `Event`, and consumes `TravelerReportData`.

- `WeatherRepository`
  Responsibility: OpenWeather communication and JSON parsing.
  Real behavior:
  - Validates that `OPEN_WEATHER_API_KEY` exists.
  - Loads current conditions from the configured weather endpoint.
  - Derives the forecast endpoint by changing `/weather` to `/forecast`.
  - Parses current weather and hourly forecast items into app models.
  Direct relationships:
  - Uses `ApiConfig.OPEN_WEATHER_API_BASE` and `ApiConfig.OPEN_WEATHER_API_KEY`.
  - Uses `HttpUtils`.
  - Creates `WeatherCurrent` and `WeatherHourlyForecast`.

- `HttpUtils`
  Responsibility: shared low-level GET and response helpers.
  Real behavior:
  - Opens JSON GET connections.
  - Validates 2xx responses.
  - Reads response bodies as UTF-8 text.
  Direct relationships:
  - Used by `ReserveNetworkRepository` and `WeatherRepository`.

- `ApiConfig`
  Responsibility: exposes API base URLs and API keys from generated `BuildConfig`.
  Real behavior:
  - Keeps keys/URLs out of source code.
  Direct relationships:
  - Used by both repositories.

### Domain models and utilities

- `Reserve`
  Responsibility: immutable reserve model.
  Real behavior:
  - Stores reserve identity, display name, optional center point, and rectangular area.
  Direct relationships:
  - Created by `ReserveNetworkRepository`.
  - Used by `MainActivity`, `MapController`, and `ReserveUtils`.
  Important note:
  - The center-point fields exist but are not currently used by rendering logic.

- `AreaBounds`
  Responsibility: immutable rectangle model.
  Real behavior:
  - Stores min/max latitude and longitude.
  - Implements `contains()` for point-in-rectangle checks.
  Direct relationships:
  - Used by `Reserve` and `ReserveUtils`.

- `Event`
  Responsibility: immutable traveler-facing hazard/event model.
  Real behavior:
  - Stores reserve id, type, priority, description, and optional coordinates.
  - Converts coordinates to `LatLng` for map markers.
  Direct relationships:
  - Created by `ReserveNetworkRepository`.
  - Used by `MapController` and `ReserveUtils`.

- `TravelerReportData`
  Responsibility: immutable report-upload payload model.
  Real behavior:
  - Stores reserve id, type, reporter name, description, coordinates, and attachment URIs.
  Direct relationships:
  - Built in `MainActivity`.
  - Consumed by `ReserveNetworkRepository.submitTravelerReport()`.

- `WeatherCurrent`
  Responsibility: immutable current-weather model.
  Direct relationships:
  - Created by `WeatherRepository`.
  - Rendered by `WeatherUiController`.

- `WeatherHourlyForecast`
  Responsibility: immutable hourly-forecast model.
  Direct relationships:
  - Created by `WeatherRepository`.
  - Rendered by `WeatherUiController`.

- `ReserveUtils`
  Responsibility: app-level domain helpers.
  Real behavior:
  - Finds which reserve contains the current `LatLng`.
  - Counts visible hazards for a specific reserve or across all reserves.
  Direct relationships:
  - Used by `MainActivity`.

## XML files and what they do

### `AndroidManifest.xml`
This is the Android app manifest. It defines:

- `INTERNET`, `ACCESS_COARSE_LOCATION`, and `ACCESS_FINE_LOCATION` permissions.
- `usesCleartextTraffic="true"` so the app can reach the default local backend over plain HTTP.
- Google Maps API key metadata.
- A `FileProvider` that enables camera-captured image URIs to be shared safely.
- `MainActivity` as the launcher activity.

### `res/layout/activity_main.xml`
This is the only active screen layout in the app. It contains:

- A `DrawerLayout` root.
- A `SupportMapFragment` that fills the background.
- A top summary card for current location and hazard summary.
- Left-side floating controls for menu, recenter, north-up, and weather toggle.
- A bottom report toggle and expandable report form.
- A weather overlay card anchored over the map.
- A left navigation drawer with server status and map-layer toggles.

This file is the main UI contract for `MainActivity`. Almost every `findViewById()` in `MainActivity` binds to IDs from this file.

### `res/layout/item_event_card.xml`
This is a standalone hazard/event card layout, but it is not currently referenced anywhere in Java or XML. It looks like a planned or leftover component for a list-based hazard UI that is not active in the current implementation.

### `res/values/strings.xml`
Contains user-facing strings for:

- Status messages
- Drawer/menu labels
- Weather text
- Report form labels and validation messages
- Hazard summary text

Observed implementation detail:

- Several strings exist but are not currently used by code or active layout wiring, including `map_subheading`, `status_map_ready`, `display_weather_on`, `display_weather_off`, `nearest_reserve`, and `poi_marker_snippet`.

### `res/values/dimens.xml`, `res/values-land/dimens.xml`, `res/values-sw600dp/dimens.xml`
These files define the same dimension names for different device classes:

- `values/`: base phone defaults
- `values-land/`: tighter spacing for landscape
- `values-sw600dp/`: larger tablet/large-screen values

In practice, these control the padding, drawer width, report panel height, weather overlay size, and floating button sizing.

Observed implementation detail:

- `menu_button_margin_top` is defined in all three files but is not referenced by the current layout.

### `res/drawable/*.xml`
These are XML drawable resources, not bitmap images.

- `bg_map_round_button.xml`: default round map-control background
- `bg_map_round_button_active.xml`: active round button background
- `bg_map_round_button_inactive.xml`: inactive round button background
- `ic_menu_hamburger.xml`: vector hamburger icon
- `ic_weather_sun_cloud.xml`: vector weather icon

These are used by `activity_main.xml` and `MapToggleUiController`.

### `res/xml/file_paths.xml`
This is Android framework configuration for the `FileProvider`. It grants URI access for:

- `external-files-path` under `Pictures/`
- `cache-path` under `camera/`

`MainActivity.createCameraImageUri()` relies on this so the camera app can write a photo into app-managed storage.

## JSON files and what they do

### `res/raw/map_style_nature.json`
Current content: `[]`

This means the "POI on" style is effectively the default Google Map styling. The file exists so the helper can always load a raw style resource, but it does not apply any custom styling right now.

### `res/raw/map_style_nature_no_poi.json`
This raw JSON style hides:

- `poi`
- `transit`

`MapController.applyMapStyle()` loads this file when the POI toggle is off. So the current POI toggle affects base Google Map features, not reserve-center markers created by app code.

## Network JSON used by the app

The app also depends on JSON payloads returned by backend and weather APIs:

| Source | Endpoint/payload | Used fields |
| --- | --- | --- |
| Backend | `GET {BACKEND_API_BASE}/reserves` | `id`, `name`, `displayName`, `centerLatitude`, `centerLongitude`, `area.minLatitude`, `area.maxLatitude`, `area.minLongitude`, `area.maxLongitude` |
| Backend | `GET {BACKEND_API_BASE}/events?reserveId={id}` | `type`, `priority`, `description`, `latitude`, `longitude` |
| OpenWeather | current weather JSON | `main.temp`, `weather[0].description` or `weather[0].main` |
| OpenWeather | forecast JSON | `list[].main.temp`, `list[].weather`, `list[].dt_txt` |

Important detail:

- Traveler report submission is not JSON. It is uploaded as `multipart/form-data` with text fields plus repeated `attachments` file parts.

## `res/` folder explanation

For this project, `app/src/main/res/` is the non-Java side of the Android app:

- `layout/`: screen and reusable view structures
- `values/`: strings and dimensions
- `values-land/`, `values-sw600dp/`: screen-specific overrides
- `drawable/`: vector icons and shape backgrounds
- `raw/`: arbitrary files loaded as-is at runtime, here used for Google Map style JSON
- `xml/`: Android configuration XML, here used for the `FileProvider`

Android generates the `R` class from this folder, and the Java code references those generated IDs as `R.layout.*`, `R.string.*`, `R.drawable.*`, `R.raw.*`, and `R.id.*`.

## Gradle and build configuration

### Root-level Gradle files

- `build.gradle`: applies the Android application plugin version.
- `settings.gradle`: defines repositories and includes the `app` module.
- `gradle/wrapper/gradle-wrapper.properties`: pins the wrapper distribution to Gradle `9.0.0`.

### `app/build.gradle`
This file is central to runtime configuration:

- Namespace/application id: `com.reserve.mobile`
- `minSdk 24`, `targetSdk 34`, `compileSdk 34`
- Java 17 source/target compatibility
- Dependencies:
  - `androidx.appcompat`
  - `com.google.android.material`
  - `play-services-maps`
  - `play-services-location`
- `BuildConfig` fields:
  - `BACKEND_API_BASE`
  - `OPEN_WEATHER_API_BASE`
  - `OPEN_WEATHER_API_KEY`

Default behavior if no override is supplied:

- Backend base URL: `http://10.0.2.2:8080/api/public`
- OpenWeather base URL: `https://api.openweathermap.org/data/2.5/weather`

## Important implementation observations

These points matter because they describe the actual current state, not just the intended design:

- The app is single-activity. There are no Fragments, adapters, or secondary screens in the current implementation.
- Reserve membership is determined by a simple rectangular `AreaBounds.contains()` check, not polygon geometry.
- Hazards are loaded per reserve in sequence rather than from one aggregate endpoint.
- Hazard polling starts only after hazards successfully load and repeats every 15 seconds.
- The "POI" toggle does not currently draw reserve center markers. It only changes the Google Map style JSON.
- `Reserve.centerLatitude`, `Reserve.centerLongitude`, and `Reserve.hasCenterPoint()` are currently unused by active rendering logic.
- `item_event_card.xml` is currently unused.
- `reserve_name_text` exists in `activity_main.xml` but is hidden and not bound in `MainActivity`.

## Bottom line

The `mobile-app` folder contains a compact Android client with one screen, several focused helper classes, and a straightforward flow:

- backend data -> repositories -> immutable models
- screen state in `MainActivity`
- rendering delegated to helper classes
- Android resources under `res/`

The codebase is small and understandable, but a few resource names and stored fields describe features that are only partially implemented or not wired at all in the current app.
