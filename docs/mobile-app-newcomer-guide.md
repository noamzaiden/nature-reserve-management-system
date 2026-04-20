# Mobile App Newcomer Guide

This guide is for someone opening the Android app code for the first time and trying to understand how the pieces fit together.

Related docs:

- [Mobile App Planning Document](./mobile-app-planning.md)
- [Mobile App Programmer Guide](./mobile-app-programmer-guide.md)
- [MainActivity Workflow](./main-activity-workflow.md)
- [Android App Block Diagram](./android-app-block-diagram.md)

## What This App Does

The mobile app is an Android client for travelers.

It lets the user:

- see reserve boundaries on a Google Map
- see published hazards on the map
- detect whether the user is inside a reserve
- view weather for the current location
- send traveler reports with optional photo or video attachments

## The Fastest Way To Understand The Code

If you only have 20 to 30 minutes, read the project in this order:

1. `mobile-app/README.md`
2. `mobile-app/app/src/main/AndroidManifest.xml`
3. `mobile-app/app/src/main/res/layout/activity_main.xml`
4. `mobile-app/app/src/main/java/com/reserve/mobile/MainActivity.java`
5. `mobile-app/app/src/main/java/com/reserve/mobile/MapController.java`
6. `mobile-app/app/src/main/java/com/reserve/mobile/LocationController.java`
7. `mobile-app/app/src/main/java/com/reserve/mobile/WeatherUiController.java`
8. `mobile-app/app/src/main/java/com/reserve/mobile/ReserveStateResolver.java`
9. `mobile-app/app/src/main/java/com/reserve/mobile/ReportMediaController.java`
10. `mobile-app/app/src/main/java/com/reserve/mobile/ReportSubmissionController.java`
11. `mobile-app/app/src/main/java/com/reserve/mobile/ReserveApiClient.java`
12. `mobile-app/app/src/main/java/com/reserve/mobile/ReportApiClient.java`
13. `mobile-app/app/src/main/java/com/reserve/mobile/WeatherApiClient.java`
14. The model classes: `Reserve`, `AreaBounds`, `Event`, `TravelerReportData`, `WeatherCurrent`, `WeatherHourlyForecast`, `ReserveState`

That order gives you:

- what the app is supposed to do
- what screen exists
- what the main UI layout looks like
- where the app starts
- how the main features are split
- where server and weather data come from
- what data objects move through the app

## The Main Mental Model

The simplest way to think about the app is:

- `MainActivity` is the center of the app
- small controller classes handle focused UI behavior
- API client classes talk to the backend and weather API
- model classes hold the data

So in practice:

- `MainActivity` coordinates the screen
- controllers help it avoid becoming even larger
- API clients fetch or send data
- models are plain data objects

## The Most Important Files

### App entry and screen

- `MainActivity.java`
  The main entry point and screen coordinator. Most app behavior starts here.
- `AndroidManifest.xml`
  Declares permissions, `MainActivity`, Google Maps API key setup, and `FileProvider`.
- `activity_main.xml`
  Defines the main screen layout: map, drawer, weather card, report panel, and buttons.

### Feature helpers

- `MapController.java`
  Draws reserve polygons and hazard markers on the Google Map.
- `MapToggleUiController.java`
  Updates the UI state of the layer toggle buttons.
- `LocationController.java`
  Handles permission-result flow, last-known location lookup, and continuous device location updates.
- `WeatherUiController.java`
  Handles the weather overlay, weather caching, and weather view updates.
- `ReserveStateResolver.java`
  Computes the current reserve state from location, reserve bounds, and visible hazards.
- `EventPollingController.java`
  Repeats hazard refresh on a timer.
- `EventReportUiController.java`
  Manages the report panel and manual report-location selection on the map.
- `ReportMediaController.java`
  Handles report attachment picking, camera capture state, and selected-media text.
- `ReportSubmissionController.java`
  Handles report validation, fresh location lookup, and upload flow.

### Data access

- `ReserveApiClient.java`
  Loads reserves, hazards, and POIs from the backend.
- `ReportApiClient.java`
  Uploads traveler reports and media attachments to the backend.
- `WeatherApiClient.java`
  Loads current weather and hourly forecast from OpenWeather.

### Data models

- `Reserve.java`
  One reserve.
- `AreaBounds.java`
  A rectangular geographic area for a reserve.
- `Event.java`
  One published hazard/event.
- `ReserveState.java`
  The resolved UI-facing state for "no location", "inside reserve", or "outside reserve".
- `TravelerReportData.java`
  The user report payload before upload.
- `WeatherCurrent.java`
  Current weather data.
- `WeatherHourlyForecast.java`
  Hourly forecast item.

## How The App Starts

The startup flow is:

1. Android launches `MainActivity`.
2. `MainActivity.onCreate()` creates the main helpers, binds views, and configures launchers, buttons, and the map fragment.
3. `ReserveStateResolver`, `LocationController`, `ReportMediaController`, and `ReportSubmissionController` are part of that helper setup.
4. `MainActivity` starts reserve loading immediately from `onCreate()`.
5. The map fragment asynchronously returns a `GoogleMap` later in `onMapReady()`.
6. Once the map is ready, `MainActivity` starts location tracking through `LocationController`.
7. After reserves are loaded, the first hazard load begins.
8. The reserve-state UI, map, report-location text, and weather update as location and backend data arrive.

If you are trying to understand "what happens first", start in:

- `MainActivity.onCreate()`
- `MainActivity.loadReserves()`
- `MainActivity.onMapReady()`
- `MainActivity.loadPublishedHazards()`

## Main Runtime Flows

### 1. Reserve and hazard flow

- `MainActivity.loadReserves()` calls `ReserveApiClient.loadReserves()`
- `ReserveApiClient` calls the backend `/reserves`
- JSON is converted into `Reserve` objects
- `MainActivity.loadPublishedHazards()` calls `ReserveApiClient.loadPublishedHazards(reserves)`
- hazard JSON is converted into `Event` objects
- `MapController.refresh(...)` redraws the map

### 2. Location and reserve detection flow

- `MainActivity.startLocationTracking()` delegates to `LocationController`
- `LocationController` handles permission requests, last-known location, and live updates
- each new device location updates `currentUserLatLng`
- `ReserveStateResolver` computes the current reserve state
- `MainActivity.updateReserveState()` renders the state into the UI
- the app updates the location text, selected reserve, report location text, map, and weather

### 3. Weather flow

- user taps the weather button
- `MainActivity` calls `WeatherUiController.refreshWeather(...)`
- `WeatherUiController` decides whether cached weather is still good
- if needed, it asks `WeatherApiClient` for fresh data
- `WeatherApiClient` calls OpenWeather and creates `WeatherCurrent` and `WeatherHourlyForecast`
- `WeatherUiController` updates the weather views

### 4. Report submission flow

- user opens the report panel
- user fills in reserve, type, description, and optional media
- user can optionally pick a manual map location
- `ReportMediaController` manages attachment and camera selection state
- `MainActivity.submitTravelerReport()` passes the form state to `ReportSubmissionController`
- `ReportSubmissionController` validates the form and resolves the best report location
- `ReportSubmissionController` builds `TravelerReportData`
- `ReportApiClient.submitTravelerReport(...)` uploads the report
- after success, the form is cleared and hazards are refreshed

## Who Owns What

This is useful when you are deciding where to change code.

- `MainActivity`
  Owns most screen state, like reserves, hazards, current location, and the current reserve state shown in the UI.
- `MapController`
  Owns map-layer rendering behavior.
- `LocationController`
  Owns permission-result handling and continuous device location tracking.
- `ReserveStateResolver`
  Owns reserve-state calculation from location and hazards.
- `WeatherUiController`
  Owns weather UI state and weather cache state.
- `EventReportUiController`
  Owns report-panel visibility and manual report-location state.
- `ReportMediaController`
  Owns selected attachment state and camera/picker result handling.
- `ReportSubmissionController`
  Owns report validation, fresh report-location lookup, and upload orchestration.
- `EventPollingController`
  Owns only the polling timer behavior.
- API clients
  Own data fetching and upload code.

## Threads and Async Work

There is no explicit `new Thread(...)` in the app right now.

Instead, the app mainly uses:

- one `ExecutorService` in `MainActivity`
- Android callbacks
- one `Handler` in `EventPollingController`

Important points:

- `MainActivity` uses the executor to load reserves, hazards, and send reports.
- `WeatherUiController` uses the same executor to load weather.
- `EventPollingController` uses a `Handler` on the main thread to schedule repeated polling.
- after background work finishes, the code returns to the UI with `runOnUiThread(...)`.

So the rough pattern is:

1. UI event happens
2. app schedules background work with `executorService.execute(...)`
3. API client performs network request
4. app returns to the UI thread with `runOnUiThread(...)`

## External Libraries and Services

From `mobile-app/app/build.gradle`, the app uses:

- `androidx.appcompat`
- `com.google.android.material`
- `com.google.android.gms:play-services-maps`
- `com.google.android.gms:play-services-location`

External services:

- backend API at `BuildConfig.BACKEND_API_BASE`
- OpenWeather API at `BuildConfig.OPEN_WEATHER_API_BASE`
- Google Maps SDK through the Maps dependency and manifest API key

## Common Questions A Newcomer Usually Has

### Where is the main logic?

Mostly in `MainActivity.java`.

### Where do hazards come from?

From `ReserveApiClient.loadPublishedHazards(...)`.

### Where is map drawing done?

In `MapController.java`.

### Where is weather fetched?

In `WeatherApiClient.java`.

### Why is the weather logic not in `MainActivity`?

Because `WeatherUiController` owns weather-specific UI and caching behavior.

### Where is the report-panel behavior?

In `EventReportUiController.java`.

### Where are the API URLs and keys?

They come from generated `BuildConfig` fields defined in `mobile-app/app/build.gradle`.

## Best Places To Make Common Changes

### Change the main UI layout

Edit:

- `activity_main.xml`
- `strings.xml`
- `dimens.xml`

### Change how the map is drawn

Edit:

- `MapController.java`
- raw map style files in `res/raw`

### Change hazard loading or report upload

Edit:

- `ReserveApiClient.java`
- `ReportApiClient.java`
- `ReportSubmissionController.java`

### Change weather fetch or weather parsing

Edit:

- `WeatherApiClient.java`

### Change weather UI behavior

Edit:

- `WeatherUiController.java`

### Change report-panel behavior

Edit:

- `EventReportUiController.java`

### Change overall feature wiring

Edit:

- `MainActivity.java`

## Things That May Confuse You

### 1. `MainActivity` is large on purpose

This project uses one main screen, so a lot of orchestration lives there. That is normal for this codebase.

### 2. The controller classes are not full MVC controllers

Here, "controller" mostly means "focused helper for one feature area".

### 3. The API clients do raw HTTP directly

There is no Retrofit or OkHttp layer. The API clients use `HttpURLConnection` and `org.json` directly.

### 4. Some older notes may mention removed helper classes

The current code no longer has:

- `ApiConfig.java`
- `HttpUtils.java`
- `ReserveUtils.java`
- `ReserveNetworkRepository.java`
- `WeatherRepository.java`

Their responsibilities are now split more clearly across:

- `ReserveApiClient`, `ReportApiClient`, and `WeatherApiClient` for network work
- `LocationController`, `ReserveStateResolver`, `ReportMediaController`, and `ReportSubmissionController` for focused workflows
- `MainActivity` for overall screen orchestration

## Suggested First Reading Session

If you are onboarding a teammate, a good first reading session is:

1. Read `mobile-app/README.md`
2. Open `MainActivity.java` and follow `onCreate()`
3. Read `activity_main.xml` side by side with `bindViews()`
4. Trace one flow end to end:
   - reserves and hazards
   - or weather
   - or report submission
5. Read the matching controller and API client for that flow
6. Only then read the planning doc and block diagram

## Short Summary

If you remember only one thing, remember this:

`MainActivity` is the coordinator, controllers handle focused UI behavior, API clients handle reserve data, report upload, and weather access, and models carry data between them.
