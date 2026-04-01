# Mobile App

Android client for travelers who want to view reserve hazards on a map, detect which reserve they are in, and submit reports with optional media attachments.

## Features

- Show a Google Map that follows the traveler location
- Detect whether the traveler is currently inside a known reserve
- Display published traveler-facing hazards on the map
- Submit traveler reports with photos or videos
- Uses `10.0.2.2` so an Android emulator can reach the local backend on the host machine

## Run

1. Open `mobile-app` in Android Studio.
2. Add a Google Maps Android API key to `mobile-app/gradle.properties` as `MAPS_API_KEY=...`.
3. Start an emulator.
4. Run the `app` configuration.

The backend must already be running on `http://localhost:8080`.
