# Windows Setup Guide

Use this guide to run the full project on Windows:

- PostgreSQL in Docker
- Spring Boot backend
- React web app
- Android app in Android Studio

## What You Need

Install these first:

- Java 17
- Node.js and npm
- Docker Desktop
- Android Studio
- An Android emulator

## One-Time Setup

Install web dependencies:

```powershell
cd web-app
npm install
cd ..
```

Add the Android keys:

```cmd
.\setup-android-keys.cmd
```

This writes the Android app values to `mobile-app\.gradle-user\gradle.properties` relative to the script, so it targets the Android project inside `mobile-app`.
The mobile app wrapper now defaults `GRADLE_USER_HOME` to `mobile-app\.gradle-user`, so local Gradle builds use that same folder for both wrapper files and personal properties.

If you want to pass the keys directly:

```cmd
.\setup-android-keys.cmd -MapsApiKey "YOUR_GOOGLE_MAPS_API_KEY" -OpenWeatherApiKey "YOUR_OPENWEATHER_API_KEY"
```

## Run The Project

1. Start Docker Desktop.
2. From the repository root, run:

```powershell
.\run-backend.cmd
```

This starts PostgreSQL and opens the backend and web app in separate windows.

3. Open Android Studio.
4. Open the `mobile-app` folder.

> Screenshot placeholder: Android Studio open-project screen with the `mobile-app` folder selected.
> Suggested image path: `docs/images/windows/android-studio-open-project.png`

5. Wait for Gradle sync.
6. Open `Device Manager` and start an emulator.

> Screenshot placeholder: Android Studio Device Manager with the emulator start button visible.
> Suggested image path: `docs/images/windows/android-studio-device-manager.png`

7. Run the `app` configuration.

> Screenshot placeholder: Android Studio toolbar showing the selected emulator and the `app` run configuration.
> Suggested image path: `docs/images/windows/android-studio-run-app.png`

## Default Local Addresses

- Backend API: `http://localhost:8080`
- Web app: `http://localhost:5173`
- Android emulator to backend: `http://10.0.2.2:8080/api/public`

Admin login:

- Email: `admin@reserve.local`
- Password: `ChangeMe123!`

## Quick Check

- Open `http://localhost:8080/api/public/reserves` and confirm you get JSON.
- Open `http://localhost:5173` and confirm the web app loads.
- In the emulator, confirm the Android app loads reserve data.

## Stop Everything

- Close the backend and web-app windows opened by `run-backend.cmd`
- Stop the Android app or emulator in Android Studio
- Stop PostgreSQL:

```powershell
cd backend
docker compose down
cd ..
```

## Notes

- If `run-backend.cmd` fails, make sure `java`, `npm`, and `docker` are available in `PATH`.
- If Android Studio asks to install missing SDK components, install them and run again.
