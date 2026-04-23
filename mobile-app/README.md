# Mobile App

Android client for travelers who want to view reserve hazards on a map, detect which reserve they are in, and submit reports with optional media attachments.

Helpful docs:

- `../docs/mobile-app-presentation-study-guide.md`
- `../docs/mobile-app-newcomer-guide.md`
- `../docs/mobile-app-programmer-guide.md`
- `../docs/mobile-app-file-reference-table.md`
- `../docs/main-activity-workflow.md`
- `../docs/mobile-app-planning.md`
- `../docs/android-app-block-diagram.md`
- `../docs/system-user-guide.md`

## Features

- Show a Google Map that follows the traveler location
- Detect whether the traveler is currently inside a known reserve
- Display published traveler-facing hazards on the map
- Submit traveler reports with photos or videos
- Uses `10.0.2.2` so an Android emulator can reach the local backend on the host machine

## Current Structure

- `MainActivity` is the screen coordinator and owns shared activity state.
- Focused helper classes keep feature workflows out of the activity:
  `MapController`, `MapToggleUiController`, `EventPollingController`,
  `EventReportUiController`, `LocationController`, `ReportMediaController`,
  `ReportSubmissionController`, `ReserveStateResolver`, and `WeatherUiController`.
- `ReserveApiClient`, `ReportApiClient`, and `WeatherApiClient` handle network calls.
- Model classes (`Reserve`, `Event`, `ReserveState`, `TravelerReportData`, and weather models)
  carry the data moving through the app.

## Run

1. Open `mobile-app` in Android Studio.
2. Add your local API values in your user Gradle properties (`%USERPROFILE%\\.gradle\\gradle.properties`) so they stay out of Git:
   - `MAPS_API_KEY=...`
   - `OPEN_WEATHER_API_KEY=...`
   - Optional overrides:
     `BACKEND_API_BASE=...`
     `OPEN_WEATHER_API_BASE=...`
   See `mobile-app/gradle.properties.example` for the expected keys.
3. Start an emulator.
4. Run the `app` configuration.

`mobile-app/gradle.properties` keeps shared non-secret Gradle settings. Put personal API values in your user Gradle properties so they do not get committed.

The app targets `http://localhost:8080` through `10.0.2.2` on the Android emulator. If the backend starts after the app, the mobile client now retries automatically and reconnects when the server becomes available.
