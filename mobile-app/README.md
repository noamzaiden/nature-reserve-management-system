# Mobile App

Android client for travelers who want to view reserve hazards on a map, detect which reserve they are in, and submit reports with optional media attachments.

Helpful docs:

- `../docs/mobile-app-newcomer-guide.md`
- `../docs/main-activity-workflow.md`
- `../docs/mobile-app-planning.md`
- `../docs/android-app-block-diagram.md`

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
- `ReserveService` and `WeatherService` handle backend and weather HTTP calls.
- Model classes (`Reserve`, `Event`, `ReserveState`, `TravelerReportData`, and weather models)
  carry the data moving through the app.

## Run

1. Open `mobile-app` in Android Studio.
2. Add API keys to `mobile-app/gradle.properties`:
   - `MAPS_API_KEY=...`
   - `OPEN_WEATHER_API_KEY=...`
   You can also put them in your user Gradle properties (`%USERPROFILE%\\.gradle\\gradle.properties`) to keep local secrets out of project files.
3. Start an emulator.
4. Run the `app` configuration.

The backend must already be running on `http://localhost:8080`.
