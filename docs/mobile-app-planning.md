# Mobile App Planning Document

## Purpose

This document summarizes the current structure of the Android `mobile-app` module, describes the main classes and how they relate to each other, and proposes a practical plan for improving the codebase without changing product behavior.

Newcomer companion: [Mobile App Newcomer Guide](./mobile-app-newcomer-guide.md)
Diagram companion: [Android App Block Diagram](./android-app-block-diagram.md)

The app is a single-activity Android client for travelers. It shows reserve boundaries and published hazards on a Google Map, tracks the user location, displays weather for the current position, and allows the user to submit traveler reports with optional media attachments.

## High-Level Structure

The mobile app is organized around one central screen and a small set of helper classes:

- `MainActivity` is the composition root and runtime orchestrator.
- Helpers encapsulate focused screen or workflow behavior:
  - `MapController`
  - `MapToggleUiController`
  - `LocationController`
  - `ReserveStateResolver`
  - `WeatherUiController`
  - `EventPollingController`
  - `EventReportUiController`
  - `ReportMediaController`
  - `ReportSubmissionController`
- Services handle external data access:
  - `ReserveService`
  - `WeatherService`
- Model classes hold reserve, hazard, weather, and report data:
  - `Reserve`
  - `AreaBounds`
  - `Event`
  - `TravelerReportData`
  - `WeatherCurrent`
  - `WeatherHourlyForecast`
  - `ReserveState`

## Architectural View

The current architecture is best described as a single-screen controller architecture with extracted helper classes instead of a formal MVVM or Clean Architecture split.

- Presentation layer:
  - `MainActivity`
  - `activity_main.xml`
  - UI helper controllers
- Domain-ish logic:
  - `ReserveStateResolver`
  - `ReportSubmissionController`
  - `MainActivity` render and orchestration helpers
- Data/integration layer:
  - `ReserveService`
  - `WeatherService`
  - service-local HTTP helper methods
  - generated `BuildConfig` values
- External services:
  - Google Maps SDK
  - Fused Location Provider
  - backend REST API
  - OpenWeather API

## Main Runtime Flows

### 1. App startup and map initialization

1. `MainActivity.onCreate()` binds views, configures launchers and buttons, builds helper controllers, and starts reserve loading.
2. `SupportMapFragment` asynchronously returns the `GoogleMap` instance through `onMapReady()`.
3. `MapController.attachMap()` applies the initial camera and map style.
4. `MainActivity` starts location tracking and refreshes the map once reserve and hazard data arrive.

### 2. Reserve and hazard loading

1. `MainActivity.loadReserves()` runs on a background executor.
2. `ReserveService.loadReserves()` calls the backend `/reserves` endpoint.
3. Response JSON is mapped into `Reserve` and `AreaBounds`.
4. `MainActivity.loadPublishedHazards()` calls `ReserveService.loadPublishedHazards(reserves)`.
5. Hazards are fetched per reserve and mapped into `Event`.
6. `MapController.refresh()` redraws reserve polygons and visible hazard markers.
7. `EventPollingController` keeps hazard refresh running on a fixed interval.

### 3. Location and reserve detection

1. `MainActivity.startLocationTracking()` delegates to `LocationController`.
2. `LocationController` manages permission-result flow, last-known location lookup, and live updates.
3. The resolved location is applied back into `MainActivity`.
4. `ReserveStateResolver` computes the current `ReserveState`, and `MainActivity.updateReserveState()` renders it.
5. Location hint text, hazard-count text, reserve spinner selection, report location text, map rendering, and weather display are refreshed from that state.

### 4. Weather overlay

1. The weather toggle in `MainActivity` enables the overlay.
2. `WeatherUiController.refreshWeather()` decides whether to use cached data or reload.
3. `WeatherService` calls OpenWeather current and forecast endpoints.
4. Responses are mapped into `WeatherCurrent` and `WeatherHourlyForecast`.
5. `WeatherUiController` updates compact and expanded weather UI states.

### 5. Traveler report submission

1. The user opens the report panel, fills the form, and optionally selects media or a manual map location.
2. `EventReportUiController` manages report panel state and manual report marker state.
3. `ReportMediaController` manages selected attachments and camera capture state.
4. `MainActivity.submitTravelerReport()` delegates validation, fresh-location lookup, and upload orchestration to `ReportSubmissionController`.
5. `ReportSubmissionController` creates `TravelerReportData` from the form inputs and selected attachments.
6. `ReserveService.submitTravelerReport()` sends a multipart request to the backend.
7. On success, host callbacks clear the form, close the panel, and reload hazards.

## Class Catalog

### Entry point and orchestration

#### `MainActivity`

Role:
Owns the screen lifecycle, binds Android views, wires controllers and services together, coordinates map/location/network/report/weather flows, and stores most screen state.

Key responsibilities:

- Creates and owns the app executor.
- Initializes Google Maps and fused location services.
- Loads reserves and published hazards.
- Starts and stops periodic hazard polling.
- Tracks current user location and the current `ReserveState`.
- Coordinates report submission and media capture.
- Delegates focused behavior to helper controllers.

Important dependencies:

- `MapController`
- `MapToggleUiController`
- `LocationController`
- `ReserveStateResolver`
- `WeatherUiController`
- `EventPollingController`
- `EventReportUiController`
- `ReportMediaController`
- `ReportSubmissionController`
- `ReserveService`
- `WeatherService`
- Android location and activity result APIs

Assessment:
`MainActivity` is the most important class in the app and also the main concentration of complexity. It currently acts as screen controller, state holder, workflow coordinator, and partial business-logic layer.

### UI helper controllers

#### `MapController`

Role:
Owns Google Map rendering concerns.

Responsibilities:

- Attaches the `GoogleMap` instance.
- Applies map style based on POI visibility.
- Draws reserve boundary polygons.
- Draws hazard markers.

Relations:

- Reads `Reserve`, `AreaBounds`, and `Event`.
- Is driven by `MainActivity`.
- Exposes toggle state used by `MapToggleUiController`.

#### `MapToggleUiController`

Role:
Updates button labels and button styling for map-layer controls.

Responsibilities:

- Reflects current POI visibility.
- Reflects current hazard visibility.
- Reflects current weather toggle state.

Relations:

- Reads toggle state from `MapController`.
- Updates UI components owned by `MainActivity`.

#### `LocationController`

Role:
Owns device-location tracking behavior that used to live directly in `MainActivity`.

Responsibilities:

- Handles location permission-result flow.
- Enables the map my-location layer when permission is granted.
- Requests the last known location for faster initial UI state.
- Starts and stops continuous location updates.
- Reports location events back to `MainActivity`.

Relations:

- Called by `MainActivity`.
- Uses `FusedLocationProviderClient`.
- Updates `MainActivity` through host callbacks.

#### `ReserveStateResolver`

Role:
Keeps reserve-state calculation out of `MainActivity`.

Responsibilities:

- Finds whether the current location is inside a known reserve.
- Counts visible hazards for either one reserve or all reserves.
- Builds a `ReserveState` object used by the UI layer.

Relations:

- Called by `MainActivity`.
- Reads `Reserve`, `AreaBounds`, `Event`, and `LatLng`.

#### `WeatherUiController`

Role:
Owns weather overlay state, caching rules, and rendering.

Responsibilities:

- Maintains expanded/collapsed weather state.
- Decides when weather data should be reloaded.
- Caches weather by age and movement distance.
- Delegates network access to `WeatherService`.
- Renders current and hourly weather text.

Relations:

- Called by `MainActivity`.
- Uses `WeatherService`.
- Stores `WeatherCurrent` and `WeatherHourlyForecast`.

#### `EventPollingController`

Role:
Encapsulates repeated hazard polling on the main thread.

Responsibilities:

- Starts the periodic polling loop.
- Stops the loop and removes callbacks.
- Calls a supplied polling action on each interval.

Relations:

- Configured by `MainActivity`.
- Triggers `MainActivity.loadPublishedHazards()`.

#### `EventReportUiController`

Role:
Keeps report-panel and manual-location selection behavior out of `MainActivity`.

Responsibilities:

- Shows and hides the report panel.
- Tracks whether the user is choosing a manual report location.
- Stores and clears the manual report location.
- Adds or updates the manual report marker on the map.
- Updates the report location label.

Relations:

- Called by `MainActivity`.
- Uses `GoogleMap` marker primitives.
- Coordinates with the current device location held in `MainActivity`.

#### `ReportMediaController`

Role:
Keeps media-picker and camera-attachment behavior out of `MainActivity`.

Responsibilities:

- Launches media picking with the correct MIME types.
- Tracks the pending camera capture Uri.
- Adds selected files to the current report.
- Updates the selected-media text in the report panel.
- Clears media state after a successful submission.

Relations:

- Called by `MainActivity`.
- Uses `FileProvider`.
- Supplies attachment Uris to `ReportSubmissionController`.

#### `ReportSubmissionController`

Role:
Keeps report validation, one-shot location resolution, and upload orchestration out of `MainActivity`.

Responsibilities:

- Validates report inputs.
- Reuses manual report location when available.
- Requests a fresh GPS location for report accuracy when needed.
- Builds `TravelerReportData`.
- Calls `ReserveService.submitTravelerReport()`.
- Reports success or failure back to `MainActivity`.

Relations:

- Called by `MainActivity`.
- Uses `FusedLocationProviderClient`.
- Uses `ReserveService`.
- Works alongside `EventReportUiController`.

### Data and integration layer

#### `ReserveService`

Role:
Handles backend communication for reserves, hazards, and traveler reports.

Responsibilities:

- Loads reserve data from `/reserves`.
- Loads published hazards from `/events?reserveId=...`.
- Maps JSON into `Reserve`, `AreaBounds`, and `Event`.
- Uploads `TravelerReportData` as multipart form data.

Relations:

- Uses `BuildConfig` for backend base URL.
- Contains its own small GET helper methods.
- Is called by `MainActivity` and `ReportSubmissionController`.

Assessment:
This service centralizes backend access well, but it still performs both transport logic and JSON mapping directly in one class.

#### `WeatherService`

Role:
Handles weather API communication and JSON mapping.

Responsibilities:

- Validates whether an OpenWeather API key is present.
- Loads current weather.
- Loads hourly forecast.
- Maps JSON into `WeatherCurrent` and `WeatherHourlyForecast`.

Relations:

- Uses `BuildConfig` and its own small GET helper methods.
- Is called by `WeatherUiController`.

### Models

#### `Reserve`

Role:
Represents one reserve for map, spinner, and location matching.

Contains:

- reserve identity
- display name
- center coordinates
- rectangular bounds via `AreaBounds`

Relations:

- Built by `ReserveService`.
- Consumed by `MainActivity` and `MapController`.

#### `AreaBounds`

Role:
Represents a rectangular reserve boundary.

Responsibilities:

- Stores min/max latitude and longitude.
- Provides `contains()` for point-in-rectangle testing.

Relations:

- Owned by `Reserve`.
- Used by `ReserveStateResolver` and `MapController`.

#### `Event`

Role:
Represents one published hazard/event.

Responsibilities:

- Stores reserve ownership, type, priority, description, and coordinates.
- Converts coordinates to `LatLng`.

Relations:

- Built by `ReserveService`.
- Consumed by `MapController` and `MainActivity`.

#### `TravelerReportData`

Role:
Represents one traveler report before upload.

Responsibilities:

- Stores reserve id, type, reporter name, description, coordinates, and attachment URIs.

Relations:

- Built by `ReportSubmissionController`.
- Uploaded by `ReserveService`.

#### `ReserveState`

Role:
Represents the resolved UI state for location-aware reserve messaging.

Responsibilities:

- Stores whether the app currently has no location, is inside a reserve, or is outside all reserves.
- Carries the active reserve when one is matched.
- Carries the visible hazard count for the current state.

Relations:

- Built by `ReserveStateResolver`.
- Stored by `MainActivity`.
- Read when rendering location hint and hazard-count UI.

#### `WeatherCurrent`

Role:
Simple current-weather data object.

Relations:

- Built by `WeatherService`.
- Consumed by `WeatherUiController`.

#### `WeatherHourlyForecast`

Role:
Simple hourly-weather data object.

Relations:

- Built by `WeatherService`.
- Consumed by `WeatherUiController`.

## Dependency Map

```text
MainActivity
|- MapController
|- MapToggleUiController
|- WeatherUiController
|  `- WeatherService
|- EventPollingController
|- EventReportUiController
|- LocationController
|- ReportMediaController
|- ReportSubmissionController
|  `- ReserveService
|- ReserveStateResolver
|- ReserveService
`- WeatherService

ReserveStateResolver -> Reserve, AreaBounds, Event, ReserveState
ReserveService -> Reserve, AreaBounds, Event, TravelerReportData
WeatherService -> WeatherCurrent, WeatherHourlyForecast
MapController -> Reserve, AreaBounds, Event
```

## Relations by Responsibility

### UI orchestration

- `MainActivity` is the only Android screen.
- All other app classes support `MainActivity`.
- No fragments, view models, or navigation graph are currently used.

### State ownership

- `MainActivity` owns most runtime state:
  - reserves
  - hazards
  - current user location
  - current reserve state
  - weather visibility state
- `WeatherUiController` owns weather-specific cached state.
- `EventReportUiController` owns manual report-location UI state.
- `ReportMediaController` owns selected media state.
- `MapController` owns map-layer visibility state.

### Data flow direction

- User actions start in `MainActivity`.
- `MainActivity` delegates to controllers or services.
- Services fetch/map raw data into model objects.
- Model objects flow back to `MainActivity` and helper controllers for rendering.

## Current Strengths

- The app is small and understandable.
- Responsibilities have already started to separate into helper controllers.
- Data models are simple and focused.
- Weather logic is reasonably isolated from the rest of the screen.
- Backend and weather access are separated into dedicated services.

## Current Constraints and Design Risks

### 1. `MainActivity` is a bottleneck

The largest architectural risk is the amount of responsibility held by `MainActivity`. It currently combines:

- lifecycle handling
- permission and location coordination
- network orchestration
- state mutation
- map coordination
- report submission flow
- UI refresh logic

This makes future changes slower and increases regression risk.

### 2. State is spread across several mutable owners

The screen state is split across the activity and helper controllers without a formal state model. This works at current size, but it makes it harder to reason about refresh ordering and UI consistency.

### 3. Networking and JSON parsing are tightly coupled

The services both make HTTP calls and parse transport JSON. That is acceptable for a small prototype, but it will become harder to test and evolve when backend payloads change.

### 4. Limited testability

Most behavior is tied to Android framework classes or direct concrete implementations. There are no obvious seams for unit tests around:

- reserve loading and mapping
- report submission workflow
- location-to-reserve transitions
- polling behavior

### 5. Polling and refresh logic are imperative

Hazard refresh, weather refresh, and reserve-state refresh are coordinated through direct method calls. This keeps the code simple, but it creates hidden coupling between UI updates and background work.

## Recommended Planning Direction

### Phase 1: stabilize current design

Goal:
Keep the current feature set and reduce risk without a large rewrite.

Recommended work:

- Document the current module boundaries and keep this document updated.
- Extract a `MainScreenState` style object or equivalent state holder.
- Move reserve/hazard loading orchestration into a dedicated coordinator or presenter.
- Keep report submission logic centered in `ReportSubmissionController` instead of drifting back into `MainActivity`.
- Consider stronger typing for report types and hazard priorities if the protocol continues to grow.

### Phase 2: improve testability

Goal:
Make core workflows easier to validate without Android UI tests for everything.

Recommended work:

- Introduce interfaces for services.
- Isolate JSON mapping into mapper classes.
- Add unit tests for:
  - reserve matching and hazard counting logic in `ReserveStateResolver`
  - `WeatherUiController` cache/reload decisions
  - `ReportSubmissionController`
  - `EventPollingController`
  - service JSON parsing

### Phase 3: formalize presentation architecture

Goal:
Reduce activity complexity and make screen state explicit.

Recommended work:

- Introduce a `ViewModel` or presenter layer.
- Shift mutable screen state out of `MainActivity`.
- Convert UI refreshes from scattered imperative calls into state-driven rendering.

### Phase 4: prepare for feature growth

Goal:
Support additional screens or more advanced traveler workflows.

Recommended work:

- Split reporting, map browsing, and weather into clearer feature modules.
- Add explicit DTO/domain/UI model separation if backend payloads grow.
- Consider moving from one large activity to fragment-based or navigation-based flows if new screens are added.

## Suggested Near-Term Refactor Targets

If the team wants a practical next pass, the best first targets are:

1. Extract a `HazardDataController` or similar class from `MainActivity` for reserve/hazard loading and refresh.
2. Introduce a small screen-state holder for reserve state, server status, and weather visibility.
3. Introduce constants or enum-like wrappers for report types and hazard priorities.
4. Add tests around `ReserveStateResolver`, `ReportSubmissionController`, `EventPollingController`, and service mapping logic.
5. Keep `LocationController`, `ReportMediaController`, `ReportSubmissionController`, and `EventReportUiController` as focused helpers and avoid pushing more logic back into `MainActivity`.

## Summary

The mobile app already has a reasonable small-project shape: one activity, several focused controllers, two services, and a compact set of models. The main architectural issue is not fragmentation but centralization, because `MainActivity` currently owns too many workflows.

The best plan is to preserve the current behavior, continue separating workflow-specific logic out of `MainActivity`, make state ownership more explicit, and add tests around the non-UI logic before adding major new features.
