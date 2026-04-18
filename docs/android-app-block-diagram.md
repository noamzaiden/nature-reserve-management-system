# Android App Block Diagram

This file contains a text block diagram for the Android client only.

## Android App Block Diagram

```text
                                     +----------------------+
                                     |       Traveler       |
                                     +----------+-----------+
                                                |
                                                v
                             +------------------+------------------+
                             |              MainActivity           |
                             |-------------------------------------|
                             | - binds activity_main.xml           |
                             | - handles permissions               |
                             | - handles location updates          |
                             | - coordinates map/weather/reporting |
                             | - starts reserve and event loading  |
                             +----+-----------+----------+---------+
                                  |           |          |
                                  |           |          |
                    +-------------+           |          +------------------+
                    |                         |                             |
                    v                         v                             v
        +-----------+----------+   +----------+-----------+     +-----------+-----------+
        |     Map Module       |   |    Weather Module    |     |     Report Module     |
        |----------------------|   |----------------------|     |-----------------------|
        | MapController        |   | WeatherUiController  |     | EventReportUiController|
        | MapToggleUiController|   | WeatherRepository    |     | TravelerReportData    |
        | reserve polygons     |   | WeatherCurrent       |     | form state            |
        | reserve markers      |   | WeatherHourlyForecast|     | manual map location   |
        | event markers        |   | weather overlay      |     | report payload        |
        +-----------+----------+   +----------+-----------+     +-----------+-----------+
                    |                         |                             |
                    |                         |                             |
                    v                         v                             |
        +-----------+----------+   +----------+-----------+                 |
        |    Google Maps SDK   |   |   OpenWeather API    |                 |
        +----------------------+   +----------------------+                 |
                                                                              |
                              +---------------------------+                   |
                              |       Event Module        |<------------------+
                              |---------------------------|
                              | EventPollingController    |
                              | Event                     |
                              | periodic event refresh    |
                              +-------------+-------------+
                                            |
                                            v
                              +-------------+-------------+
                              |   Reserve Data Module     |
                              |---------------------------|
                              | ReserveNetworkRepository  |
                              | Reserve                   |
                              | AreaBounds                |
                              | fetches public reserves   |
                              | fetches public events     |
                              | uploads traveler reports  |
                              +-------------+-------------+
                                            |
                                            v
                              +-------------+-------------+
                              |    Backend Public API     |
                              |---------------------------|
                              | GET /api/public/reserves  |
                              | GET /api/public/events    |
                              | POST /api/public/reports  |
                              | GET /api/public/media     |
                              +---------------------------+


         +------------------------------+      +------------------------------+
         | Utility / Config Module      |      | Android Resources            |
         |------------------------------|      |------------------------------|
         | ReserveUtils                 |      | activity_main.xml            |
         | HttpUtils                    |      | AndroidManifest.xml          |
         | ApiConfig                    |      | res/layout                   |
         | geo + HTTP + config helpers  |      | res/values                   |
         +---------------+--------------+      | res/xml                      |
                         ^                     | res/raw                      |
                         |                     +------------------------------+
                         +---------------------------------------------------+
                                            used by MainActivity/modules


         +------------------------------+      +------------------------------+
         | Google Play Services Location|----->| MainActivity                 |
         | fused location provider      |      | currentUserLatLng            |
         +------------------------------+      +------------------------------+
```

## Main Runtime Flows

### 1. Reserve Loading

```text
MainActivity
-> executorService
-> ReserveNetworkRepository.loadReserves()
-> Backend Public API /api/public/reserves
-> Reserve + AreaBounds
-> MainActivity stores reserves
-> Map Module renders reserve areas
```

### 2. Event Loading

```text
MainActivity
-> executorService
-> ReserveNetworkRepository.loadPublishedHazards(reserves)
-> Backend Public API /api/public/events?reserveId=...
-> Event
-> MainActivity stores allHazards
-> Map Module renders event markers
-> ReserveUtils helps build reserve summary
```

### 3. Weather Loading

```text
MainActivity.refreshWeather(...)
-> WeatherUiController
-> executorService
-> WeatherRepository
-> OpenWeather API
-> WeatherCurrent + WeatherHourlyForecast
-> WeatherUiController updates weather overlay
```

### 4. Report Submission

```text
Traveler fills report UI
-> EventReportUiController
-> MainActivity
-> TravelerReportData
-> executorService
-> ReserveNetworkRepository.submitTravelerReport(...)
-> Backend Public API /api/public/reports
-> MainActivity updates status and reloads events
```

### 5. Location Flow

```text
Google Play Services Location
-> MainActivity
-> currentUserLatLng
-> ReserveUtils determines current reserve
-> Map Module refreshes reserve context
-> WeatherUiController may reload weather if cache is stale or location changed enough
```

## Threading Summary

```text
UI thread:
- MainActivity
- button clicks
- map callbacks
- permission callbacks
- location callbacks
- view updates

Background worker:
- single ExecutorService
- reserve loading
- event loading
- weather loading
- report upload

Main-thread scheduled polling:
- EventPollingController
- periodically triggers event refresh
```
