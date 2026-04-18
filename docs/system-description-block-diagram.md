# System Description Block Diagram

This file describes the full system as a text block diagram, not just the Android app.

## System-Level Block Diagram

```text
                                     +----------------------+
                                     |    Administrator     |
                                     +----------+-----------+
                                                |
                                                |
                                     +----------v-----------+
                                     |    Web App (React)   |
                                     |----------------------|
                                     | App.jsx              |
                                     | Admin dashboard      |
                                     | login/signup         |
                                     | reserve catalog view |
                                     | assignments          |
                                     +----------+-----------+
                                                |
                                                |
                           +--------------------v--------------------+
                           |         Backend API (Spring Boot)       |
                           |-----------------------------------------|
                           | AuthController                          |
                           | AdminController                         |
                           | ReserveController                       |
                           | EventController                         |
                           | ReserveCreationRequestController        |
                           | PublicTravelerController                |
                           |-----------------------------------------|
                           | SecurityConfig + JWT filter/service     |
                           | AuthService                             |
                           | AdminService                            |
                           | ReserveService                          |
                           | EventService                            |
                           +-----+----------------------+------------+
                                 |                      |
                                 |                      |
                   +-------------v----+      +----------v------------------+
                   | PostgreSQL DB    |      | Traveler Media File Storage |
                   |------------------|      |-----------------------------|
                   | users            |      | backend/uploads/            |
                   | reserves         |      | traveler-media              |
                   | reserve requests |      | uploaded images/videos      |
                   | events           |      +-----------------------------+
                   | event media      |
                   +------------------+


                                     +----------------------+
                                     |   Reserve Manager    |
                                     +----------+-----------+
                                                |
                                                |
                                     +----------v-----------+
                                     |  Web App (React)     |
                                     |----------------------|
                                     | ManagerWorkspace.jsx |
                                     | manager console      |
                                     | reserve requests     |
                                     | event creation       |
                                     | event status control |
                                     +----------+-----------+
                                                |
                                                |
                           +--------------------v--------------------+
                           |         Backend API (Spring Boot)       |
                           +-----------------------------------------+


                                     +----------------------+
                                     |       Traveler       |
                                     +----------+-----------+
                                                |
                         +----------------------+----------------------+
                         |                                             |
                         v                                             v
            +------------+-------------+                  +------------+-------------+
            | Android Mobile App       |                  | OpenWeather API          |
            |--------------------------|                  |--------------------------|
            | MainActivity             |<---------------->| current weather          |
            | MapController            |                  | forecast                 |
            | WeatherUiController      |                  +--------------------------+
            | EventPollingController   |
            | EventReportUiController  |
            | ReserveNetworkRepository |
            +------------+-------------+
                         |
                         |
                         v
            +------------+-------------+
            | Backend Public API       |
            |--------------------------|
            | GET /api/public/reserves |
            | GET /api/public/events   |
            | POST /api/public/reports |
            | GET /api/public/media    |
            +------------+-------------+
                         |
                         v
            +------------+-------------+
            | Backend API (Spring Boot)|
            +--------------------------+


      +------------------------------+          +-------------------------------+
      | Android / Browser Platforms  |          | Build / Bootstrap Resources   |
      |------------------------------|          |-------------------------------|
      | Android runtime              |          | Flyway migrations             |
      | Google Maps SDK              |          | israeli-nature-reserves.json  |
      | Google Play Services         |          | backend application.properties|
      | Browser + Vite dev server    |          | mobile Gradle config          |
      +------------------------------+          +-------------------------------+
```

## Main System Blocks

### 1. Web App

Purpose:
- browser client for administrators and reserve managers

Main responsibilities:
- login and signup
- admin reserve catalog management
- assignment of managers to reserves
- reserve request review
- manager workspace for event creation and event updates
- map-based reserve views with Leaflet

Key files:
- [App.jsx](/d:/Java%20Workshop/nature-reserve-management-system/web-app/src/App.jsx)
- [ManagerWorkspace.jsx](/d:/Java%20Workshop/nature-reserve-management-system/web-app/src/manager/ManagerWorkspace.jsx)

### 2. Mobile App

Purpose:
- traveler-facing Android app

Main responsibilities:
- fetch public reserve list
- fetch published reserve events
- display map and weather
- allow travelers to submit reports with media

Key files:
- [MainActivity.java](/d:/Java%20Workshop/nature-reserve-management-system/mobile-app/app/src/main/java/com/reserve/mobile/MainActivity.java)
- [ReserveNetworkRepository.java](/d:/Java%20Workshop/nature-reserve-management-system/mobile-app/app/src/main/java/com/reserve/mobile/ReserveNetworkRepository.java)
- [WeatherUiController.java](/d:/Java%20Workshop/nature-reserve-management-system/mobile-app/app/src/main/java/com/reserve/mobile/WeatherUiController.java)

### 3. Backend API

Purpose:
- central application server for auth, reserve data, event workflows, and traveler public endpoints

Main responsibilities:
- JWT-based authentication and authorization
- admin APIs
- manager APIs
- public traveler APIs
- event and reserve business logic
- persistence to PostgreSQL
- traveler media upload handling

Main controller surfaces:
- `/api/auth`
- `/api/admin`
- `/api/reserves`
- `/api/events`
- `/api/reserve-requests`
- `/api/public`

Key files:
- [AuthController.java](/d:/Java%20Workshop/nature-reserve-management-system/backend/src/main/java/com/noam/fleetcommand/auth/AuthController.java)
- [AdminController.java](/d:/Java%20Workshop/nature-reserve-management-system/backend/src/main/java/com/noam/fleetcommand/admin/AdminController.java)
- [EventController.java](/d:/Java%20Workshop/nature-reserve-management-system/backend/src/main/java/com/noam/fleetcommand/events/EventController.java)
- [PublicTravelerController.java](/d:/Java%20Workshop/nature-reserve-management-system/backend/src/main/java/com/noam/fleetcommand/events/PublicTravelerController.java)

### 4. PostgreSQL Database

Purpose:
- persistent store for the backend

Stores:
- users
- reserves
- reserve creation requests
- events
- event media metadata

Bootstrapping:
- Docker Compose starts PostgreSQL
- Flyway migrations create and evolve schema

Key files:
- [docker-compose.yml](/d:/Java%20Workshop/nature-reserve-management-system/backend/docker-compose.yml)
- [application.properties](/d:/Java%20Workshop/nature-reserve-management-system/backend/src/main/resources/application.properties)

### 5. Traveler Media Storage

Purpose:
- filesystem storage for uploaded report images and videos

Path:
- `backend/uploads/traveler-media`

Backend access:
- files are saved by `TravelerMediaStorageService`
- media is exposed through `/api/public/media/{fileName}`

### 6. External Services and Platforms

Included integrations:
- OpenWeather API for traveler weather data
- Google Maps SDK in Android
- Google Play Services Location in Android
- Browser + Vite runtime for the web app

## Main Interaction Flows

### Flow 1: Admin workflow

```text
Administrator
-> Web App
-> /api/auth login
-> Backend API
-> JWT token
-> Web App stores token
-> /api/admin endpoints
-> Backend services
-> PostgreSQL
-> updated reserve assignments / request decisions
```

### Flow 2: Manager workflow

```text
Reserve Manager
-> Web App
-> /api/reserves
-> /api/events
-> /api/reserve-requests/mine
-> Backend API
-> PostgreSQL
-> manager dashboard data

Reserve Manager
-> Web App
-> POST /api/events
-> PATCH /api/events/{id}/status
-> PATCH /api/events/{id}/priority
-> PATCH /api/events/{id}/publish
-> Backend API
-> PostgreSQL
```

### Flow 3: Traveler mobile workflow

```text
Traveler
-> Android Mobile App
-> GET /api/public/reserves
-> GET /api/public/events?reserveId=...
-> Backend API
-> PostgreSQL
-> reserve + event data back to app
```

### Flow 4: Traveler report submission

```text
Traveler
-> Android Mobile App
-> POST /api/public/reports (multipart form data)
-> Backend API
-> PostgreSQL event records
-> local traveler-media file storage
-> report becomes part of manager event workflow
```

### Flow 5: Weather flow

```text
Traveler
-> Android Mobile App
-> WeatherUiController / WeatherRepository
-> OpenWeather API
-> weather JSON
-> weather overlay on mobile app
```

## System Notes

- The backend is the central integration point for both clients.
- The web app talks only to the backend, not directly to the database.
- The mobile app talks to the backend for reserve/event/report data, but talks directly to OpenWeather for weather.
- Uploaded traveler media is stored on local disk, while metadata about that media is stored in PostgreSQL.
- PostgreSQL is started separately through Docker Compose during local development.
- Reserve catalog structure is bootstrapped from migrations and reserve metadata resources in the backend.
