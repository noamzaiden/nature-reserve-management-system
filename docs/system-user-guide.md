# System User Guide

This guide explains how to run and use the full Reserve Management System as a local development or demo environment.

The system includes:

- a Spring Boot backend
- a React web dashboard for administrators and reserve managers
- an Android traveler app

## Who Uses The System

### Administrator

Uses the web dashboard to:

- review the reserve catalog
- review reserve requests
- assign or unassign managers to reserves
- monitor active and inactive reserves

### Reserve manager

Uses the web dashboard to:

- create an account
- sign in
- request reserve responsibility
- create, update, prioritize, publish, and close events
- manage reserve-facing operational data such as published traveler alerts and reserve POIs

### Traveler

Uses the Android app to:

- view reserve boundaries
- view traveler-visible hazards and POIs
- see whether they are inside a known reserve
- optionally view local weather
- submit field reports with optional media

## Local Startup

### 1. Start PostgreSQL

From the `backend` folder:

```bash
docker compose up -d
```

This starts the database on `localhost:5432`.

### 2. Start the backend

From the `backend` folder:

```bash
./mvnw spring-boot:run
```

Backend base URL:

- `http://localhost:8080`

Important backend endpoint groups:

- `/api/auth/...`
- `/api/admin/...`
- `/api/events/...`
- `/api/public/...`

### 3. Start the web dashboard

From the `web-app` folder:

```bash
npm install
npm run dev
```

Default URL:

- `http://localhost:5173`

Optional environment variable:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

### 4. Start the Android app

1. Open `mobile-app` in Android Studio.
2. Create `mobile-app/.gradle-user/gradle.properties` using `mobile-app/gradle.properties.example` as the reference.
3. Set your local values:

```properties
MAPS_API_KEY=...
OPEN_WEATHER_API_KEY=...
BACKEND_API_BASE=http://10.0.2.2:8080/api/public
OPEN_WEATHER_API_BASE=https://api.openweathermap.org/data/2.5/weather
```

4. Start an Android emulator.
5. Run the `app` configuration.

Why `10.0.2.2`:

- from the Android emulator, `localhost` points to the emulator itself
- `10.0.2.2` points back to your host machine

## Default Admin Account

Seeded administrator account:

- Email: `admin@reserve.local`
- Password: `ChangeMe123!`

Managers create their own accounts from the web sign-up screen.

## How To Use The System

## Administrator Workflow

1. Open the web dashboard in the browser.
2. Sign in with the seeded admin credentials.
3. Review the reserve catalog.
4. Review incoming reserve requests from managers.
5. Assign a manager to a reserve, or mark a reserve inactive and unassigned.
6. Use the reserve table or map views to inspect assignment state and reserve activity.

Expected outcome:

- assigned managers can operate on their reserves
- inactive reserves stop participating as active operational locations

## Manager Workflow

1. Open the web dashboard.
2. Create a manager account or sign in with an existing manager account.
3. Request assignment to a reserve if needed.
4. Once assigned, open the manager workspace.
5. Create a reserve event with its type, priority, description, and coordinates.
6. Update event status as the situation changes.
7. Publish the event when it should be visible to travelers.
8. Unpublish or close the event when it is no longer relevant.
9. Manage reserve POIs or related reserve data if your workflow requires it.

Expected outcome:

- published events become visible through the public traveler API
- unpublished or closed events stop appearing to travelers as active alerts

## Traveler Workflow In The Android App

1. Open the Android app.
2. Grant location permission when prompted.
3. Wait for reserve boundaries and hazards to load.
4. Use the floating controls:
   - menu button opens the side drawer
   - my-location button recenters the map
   - north-up button resets map rotation and tilt
   - weather button toggles the weather overlay
5. Use the drawer toggles to show or hide POIs and hazards.
6. Read the location hint card to see whether you are inside a known reserve.
7. Open the report panel if you want to submit a field report.
8. Choose the reserve, report type, and description.
9. Optionally:
   - attach media from storage
   - take a photo
   - tap the map to choose a manual report point
10. Send the report.

Expected outcome:

- the backend receives a traveler-origin event report
- uploaded attachments are stored in local development media storage
- the mobile app refreshes traveler-visible hazards afterward

## Cross-System Workflows

### Published hazard flow

1. Manager creates an event in the web dashboard.
2. Manager publishes the event.
3. Backend exposes it through `/api/public/events`.
4. Mobile app polls the public endpoint and renders the event on the map.

### Traveler report flow

1. Traveler submits a report from the Android app.
2. Mobile app uploads the report to `/api/public/reports`.
3. Backend creates a traveler-origin event and stores attachments.
4. Staff can later review and manage that event through backend-powered workflows.

### Assignment flow

1. Manager requests reserve responsibility in the web dashboard.
2. Administrator reviews the request.
3. Administrator assigns the manager to the reserve.
4. Manager gains reserve-specific operational access in the web dashboard.

## Storage And Data Notes

- PostgreSQL stores reserves, users, assignments, requests, events, and logs.
- Traveler media uploads are stored on local disk during development.
- The mobile app only talks to public endpoints and does not require authentication.
- The web dashboard uses authenticated endpoints for admin and manager workflows.

## Common Troubleshooting

### Web dashboard cannot reach backend

Check:

- backend is running on `http://localhost:8080`
- `VITE_API_BASE_URL` matches the backend URL if you changed it

### Android app shows reserve-load or hazard-load errors

Check:

- backend is running
- `BACKEND_API_BASE` is set to `http://10.0.2.2:8080/api/public` for the emulator
- emulator has network access

### Weather is unavailable in the mobile app

Check:

- `OPEN_WEATHER_API_KEY` is set in `mobile-app/.gradle-user/gradle.properties`

### Camera photo attachment fails

Check:

- the emulator or device has a working camera implementation
- the app can create files through `FileProvider`

## Recommended Demo Script

If you need to present the system in class or in a final demo:

1. Start database, backend, web app, and Android emulator.
2. Log into the web dashboard as admin.
3. Show reserve assignment or reserve request review.
4. Switch to a manager account and create a traveler-visible event.
5. Publish the event.
6. Switch to the Android app and show the hazard appearing on the map.
7. Submit a traveler report with a description and optional photo.
8. Explain that the backend stores the report and media for later staff review.

## Summary

The full system works as a loop:

- admins manage reserve ownership
- managers create and publish operational events
- travelers consume public updates and send field reports
- the backend ties those workflows together through authenticated staff APIs and public traveler APIs
