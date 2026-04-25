# Run This Project On Windows

This guide is the easiest way to get the project running on Windows.

The project has 4 parts:

- PostgreSQL in Docker
- Spring Boot backend
- React web app
- Android app in Android Studio (optional)

## What You Need

Please install these first:

- Java 17
- Node.js and npm
- Docker Desktop
- Android Studio and an emulator if you want to run the mobile app

## First-Time Setup

Open PowerShell in the project root and install the web app dependencies:

```powershell
cd web-app
npm install
cd ..
```

If you only want to run the backend and web app, you can stop here.

If you also want to run the Android app, set up the mobile API keys:

```cmd
.\setup-android-keys.cmd
```

The script will ask for:

- Google Maps API key
- OpenWeather API key

It saves them to `mobile-app\.gradle-user\gradle.properties`.

## Start The Backend And Web App

1. Start Docker Desktop.
2. In the project root, run:

```powershell
.\run-backend.cmd
```

This script will:

- start PostgreSQL
- open the Spring Boot backend in a new terminal window
- open the React web app in a new terminal window

When everything is ready, use these local addresses:

- Backend API: `http://localhost:8080`
- Web app: `http://localhost:5173`

You can quickly test the backend here:

`http://localhost:8080/api/public/reserves`

## Default Admin Login

- Email: `admin@reserve.local`
- Password: `ChangeMe123!`

## Start The Android App

This part is optional.

1. Open Android Studio.
2. Open the `mobile-app` folder.
3. Wait for Gradle sync to finish.
4. Start an emulator from Device Manager.
5. Run the `app` configuration.

The Android app is already set up to call the backend at:

`http://10.0.2.2:8080/api/public`

## Stop Everything

To stop the backend and web app, close the terminal windows opened by `run-backend.cmd`.

To stop PostgreSQL, run:

```powershell
cd backend
docker compose down
cd ..
```

## If Something Does Not Start

- Make sure Docker Desktop is running before you run `.\run-backend.cmd`.
- If the script says web dependencies are missing, run `npm install` inside `web-app`.
- If Windows cannot find `java`, `npm`, or `docker`, make sure they are installed and available in your `PATH`.
- If Android Studio asks to install missing SDK components, let it install them and then run the app again.
