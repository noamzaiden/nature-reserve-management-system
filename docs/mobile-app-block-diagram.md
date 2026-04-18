# Mobile App Text Block Diagram

This file contains a readable text block diagram for the Android client in `mobile-app`.

## High-Level Block Diagram

```text
                                     +---------------------+
                                     |        User         |
                                     +----------+----------+
                                                |
                                                v
                             +------------------+------------------+
                             |              MainActivity           |
                             |-------------------------------------|
                             | - binds activity_main.xml           |
                             | - coordinates app flow              |
                             | - handles location/permissions      |
                             | - loads reserves/events             |
                             | - submits traveler reports          |
                             +----+-----------+----------+---------+
                                  |           |          |
                   +--------------+           |          +------------------+
                   |                          |                             |
                   v                          v                             v
        +----------+-----------+   +----------+-----------+      +----------+-----------+
        |      Map Module      |   |    Weather Module    |      |     Report Module    |
        |----------------------|   |----------------------|      |----------------------|
        | MapController        |   | WeatherUiController  |      | EventReportUiController |
        | MapToggleUiController|   | WeatherRepository    |      | TravelerReportData   |
        | draws reserves       |   | WeatherCurrent       |      | report panel state   |
        | draws event markers  |   | WeatherHourlyForecast|      | manual map location  |
        | applies map style    |   | renders overlay      |      | prepares upload data |
        +----------+-----------+   +----------+-----------+      +----------+-----------+
                   |                          |                             |
                   |                          |                             |
                   v                          v                             |
        +----------+-----------+   +----------+-----------+                 |
        |   Google Maps SDK    |   |   OpenWeather API    |                 |
        +----------------------+   +----------------------+                 |
                                                                               
                                  +----------------------+                   |
                                  |     Event Module     |<------------------+
                                  |----------------------|
                                  | EventPollingController|
                                  | Event                |
                                  | periodic refresh     |
                                  | event list state     |
                                  +----------+-----------+
                                             |
                                             v
                              +--------------+---------------+
                              |      Reserve Data Module     |
                              |------------------------------|
                              | ReserveNetworkRepository     |
                              | Reserve                      |
                              | AreaBounds                   |
                              | fetches /reserves            |
                              | fetches /events              |
                              | uploads /reports             |
                              +--------------+---------------+
                                             |
                                             v
                                   +---------+----------+
                                   |   Backend REST API |
                                   +--------------------+


         +-----------------------------+      +------------------------------+
         | Utility / Config Module     |      | Android Resources            |
         |-----------------------------|      |------------------------------|
         | ReserveUtils                |      | activity_main.xml            |
         | HttpUtils                   |      | AndroidManifest.xml          |
         | ApiConfig                   |      | res/layout                   |
         | geo + HTTP + config helpers |      | res/values                   |
         +---------------+-------------+      | res/xml                      |
                         ^                    | res/raw (map style JSON)     |
                         |                    +------------------------------+
                         +---------------------------------------------------+
                                            used by MainActivity/modules


         +------------------------------+      +------------------------------+
         | Google Play Services Location|----->| MainActivity                 |
         | fused location updates       |      | updates current location     |
         +------------------------------+      +------------------------------+
```

## Main Runtime Flows

### 1. Reserve Loading

```text
User opens app
-> MainActivity
-> executorService
-> ReserveNetworkRepository.loadReserves()
-> Backend REST API /reserves
-> Reserve + AreaBounds objects
-> MainActivity stores reserve list
-> Map Module renders reserve areas
```

### 2. Event / Hazard Loading

```text
MainActivity
-> executorService
-> ReserveNetworkRepository.loadPublishedHazards(reserves)
-> Backend REST API /events?reserveId=...
-> Event objects
-> MainActivity stores allHazards
-> Map Module renders event markers
-> ReserveUtils helps build reserve summary
```

### 3. Weather Loading

```text
MainActivity.refreshWeather(...)
-> WeatherUiController
-> executorService
-> WeatherRepository.loadCurrentWeather(...)
-> WeatherRepository.loadHourlyWeather(...)
-> OpenWeather API
-> WeatherCurrent + WeatherHourlyForecast
-> WeatherUiController updates overlay text
```

### 4. Report Submission

```text
User fills report form
-> EventReportUiController
-> MainActivity
-> TravelerReportData
-> executorService
-> ReserveNetworkRepository.submitTravelerReport(...)
-> Backend REST API /reports
-> MainActivity updates status text and reloads events
```

### 5. Location Flow

```text
Google Play Services Location
-> MainActivity
-> currentUserLatLng
-> ReserveUtils finds current reserve
-> Map Module refreshes view
-> WeatherUiController may reload weather if cache is old or location changed enough
```

## Threading Summary

```text
UI thread:
- MainActivity
- button clicks
- map callbacks
- location callbacks
- view updates

Background worker thread:
- executorService
- reserve loading
- event loading
- weather loading
- report upload

Main-thread scheduled polling:
- EventPollingController
- periodically triggers event refresh
```
