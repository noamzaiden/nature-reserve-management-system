# Mobile App Block Diagram Text Specification

Use this file as source text for generating a block diagram from the Android mobile client in `mobile-app`.

This is not a class diagram. It is a higher-level architectural block diagram describing the main modules, data flow, external systems, and resource files.

## Diagram Goal

Create a block diagram for the Android mobile application.

Show:
- the main app modules
- what each module does
- how data moves between modules
- which modules talk to external services
- which XML and resource folders support the app

Keep the diagram simple and readable. Prefer blocks and directional arrows over detailed method-level design.

## Main App Blocks

### 1. MainActivity

Label:
`MainActivity`

Description:
- central orchestration block
- entry screen of the app
- binds the screen layout from `activity_main.xml`
- coordinates map, weather, reporting, polling, permissions, and location updates
- triggers reserve loading, event loading, and report submission

### 2. Map Module

Label:
`Map Module`

Contained implementation blocks:
- `MapController`
- `MapToggleUiController`

Description:
- owns Google Map presentation
- draws reserve boundaries
- draws reserve markers and event markers
- applies map style / POI visibility changes
- updates visual state of map-related toggle buttons

### 3. Weather Module

Label:
`Weather Module`

Contained implementation blocks:
- `WeatherUiController`
- `WeatherRepository`
- `WeatherCurrent`
- `WeatherHourlyForecast`

Description:
- requests weather data for the active location
- stores current weather and hourly forecast
- renders the weather overlay
- shows fallback messages when weather is unavailable

### 4. Event / Hazard Module

Label:
`Event Module`

Contained implementation blocks:
- `EventPollingController`
- `Event`

Description:
- periodically refreshes published events/hazards
- stores event data fetched from the backend
- feeds event markers and reserve summaries

### 5. Report Module

Label:
`Report Module`

Contained implementation blocks:
- `EventReportUiController`
- `TravelerReportData`

Description:
- manages the report panel UI
- supports manual location selection on the map
- collects traveler report inputs
- prepares report payload for submission

### 6. Reserve Data Module

Label:
`Reserve Data Module`

Contained implementation blocks:
- `ReserveNetworkRepository`
- `Reserve`
- `AreaBounds`

Description:
- fetches reserve list from backend API
- parses reserve JSON into app models
- fetches published events for reserves
- uploads traveler reports to backend API

### 7. Utility / Config Module

Label:
`Utility and Config Module`

Contained implementation blocks:
- `ReserveUtils`
- `HttpUtils`
- `ApiConfig`

Description:
- geographic helper logic
- HTTP response reading and validation
- backend base URL and weather API key access

### 8. Android Resources Module

Label:
`Android Resources`

Contained resource groups:
- `res/layout`
- `res/values`
- `res/xml`
- `res/raw` if map styles are stored there
- `AndroidManifest.xml`

Description:
- defines the UI layout
- defines strings, colors, and themes
- defines provider paths and XML configuration
- defines app permissions and Android component registration

## External Systems

### A. Android Framework

Description:
- activity lifecycle
- permissions
- view binding
- activity result APIs
- threading helpers

### B. Google Maps SDK

Description:
- map fragment
- map camera
- polygons and markers

### C. Google Play Services Location

Description:
- fused location provider
- live device location updates

### D. Backend REST API

Description:
- reserve list endpoint
- event list endpoint
- report upload endpoint

### E. OpenWeather API

Description:
- current weather endpoint
- forecast endpoint

## Main Data Flow

Represent the flow like this:

`User -> MainActivity -> App Modules -> Repositories/Utilities -> External APIs -> Back to UI`

Expand it into these flows.

### Flow 1: App Startup and Reserve Loading

`User opens app`
-> `MainActivity`
-> `ReserveNetworkRepository.loadReserves`
-> `Backend REST API /reserves`
-> `ReserveNetworkRepository parses JSON`
-> `Reserve + AreaBounds`
-> `MainActivity stores reserves`
-> `Map Module renders reserve areas and markers`

### Flow 2: Event/Hazard Loading

`MainActivity`
-> `ReserveNetworkRepository.loadPublishedHazards`
-> `Backend REST API /events?reserveId=...`
-> `ReserveNetworkRepository parses JSON`
-> `Event`
-> `MainActivity stores events`
-> `Map Module renders event markers`
-> `ReserveUtils counts reserve-related visible events`
-> `MainActivity updates reserve summary`

### Flow 3: Periodic Event Polling

`EventPollingController`
-> `MainActivity loadPublishedHazards action`
-> `ReserveNetworkRepository`
-> `Backend REST API`
-> `updated Event list`
-> `Map Module refresh`

### Flow 4: Weather Loading

`MainActivity`
-> `WeatherUiController`
-> `WeatherRepository`
-> `OpenWeather API`
-> `WeatherCurrent + WeatherHourlyForecast`
-> `WeatherUiController renders weather overlay`

### Flow 5: Traveler Report Submission

`User fills report form`
-> `EventReportUiController`
-> `MainActivity`
-> `TravelerReportData`
-> `ReserveNetworkRepository.submitTravelerReport`
-> `Backend REST API /reports`
-> `MainActivity updates status UI`

### Flow 6: Location Flow

`Google Play Services Location`
-> `MainActivity`
-> `current user location`
-> `ReserveUtils determines active reserve`
-> `Map Module updates map focus and reserve context`
-> `WeatherUiController may use location for weather requests`

## Block Relationships

Use these directional relationships in the block diagram:

- `User -> MainActivity`
- `MainActivity -> Map Module`
- `MainActivity -> Weather Module`
- `MainActivity -> Event Module`
- `MainActivity -> Report Module`
- `MainActivity -> Reserve Data Module`
- `MainActivity -> Utility and Config Module`
- `MainActivity -> Android Resources`

- `Map Module -> Google Maps SDK`
- `MainActivity -> Google Play Services Location`
- `Reserve Data Module -> Backend REST API`
- `Weather Module -> OpenWeather API`
- `Reserve Data Module -> Utility and Config Module`
- `Weather Module -> Utility and Config Module`
- `Event Module -> Reserve Data Module`
- `Report Module -> Reserve Data Module`
- `Report Module -> Map Module`

## Suggested Diagram Layout

Arrange blocks in this order:

Top:
- `User`

Center:
- `MainActivity`

Left side:
- `Map Module`
- `Report Module`

Right side:
- `Weather Module`
- `Event Module`

Below MainActivity:
- `Reserve Data Module`
- `Utility and Config Module`
- `Android Resources`

Bottom / external layer:
- `Backend REST API`
- `OpenWeather API`
- `Google Maps SDK`
- `Google Play Services Location`
- `Android Framework`

## Resource Notes

Attach these notes to the `Android Resources` block:

- `activity_main.xml` contains the map screen, report UI, and weather overlay UI
- `AndroidManifest.xml` defines permissions, launcher activity, Maps API metadata, and `FileProvider`
- `res/values` contains strings, colors, and themes
- `res/xml` contains provider/file path XML and related configuration
- `res/raw` may contain map style JSON files used by the map

## Threading Note

If the tool supports notes, add this runtime note:

- UI rendering and user interaction run on the Android main thread
- network operations run on a single background `ExecutorService`
- periodic event polling is scheduled from a main-thread handler

## Condensed Diagram Prompt

If a shorter prompt is needed, use this:

Create a block diagram for an Android app with `MainActivity` in the center. `MainActivity` coordinates a `Map Module`, `Weather Module`, `Event Module`, `Report Module`, `Reserve Data Module`, `Utility and Config Module`, and `Android Resources`. The `Reserve Data Module` talks to a backend REST API for reserves, events, and report upload. The `Weather Module` talks to the OpenWeather API. The `Map Module` uses Google Maps SDK. `MainActivity` receives device location from Google Play Services Location. `activity_main.xml` provides the main screen layout, while `AndroidManifest.xml` provides permissions and app component configuration.
