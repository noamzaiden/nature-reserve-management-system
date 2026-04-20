# Mobile App

Android client for travelers who want to view reserve hazards on a map, detect which reserve they are in, and submit reports with optional media attachments.

Helpful docs:

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


2. Create `mobile-app/gradle.properties` from `mobile-app/gradle.properties.example`.
3. Add your local API keys to `mobile-app/gradle.properties`:
   - `MAPS_API_KEY=...`
   - `OPEN_WEATHER_API_KEY=...`
4. You can also place these values in `%USERPROFILE%\.gradle\gradle.properties` instead of creating a project-local file.
5. Start an emulator.
6. Run the `app` configuration.

`mobile-app/gradle.properties` is for local use only and should not be committed.

The backend must already be running on `http://localhost:8080`.
