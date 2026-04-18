# Mobile App UML Text Specification

Use this file as source text for generating UML from the `mobile-app` Android client.

## Diagram Goal

Create a UML class diagram for the Android mobile client inside `mobile-app/app/src/main/java/com/reserve/mobile`.

Focus on:
- main classes
- class responsibilities
- associations between classes
- key created/owned data objects
- external systems the app depends on

Do not model every Android SDK class in detail. Treat Android and Google Maps APIs as external framework dependencies.

## Main Classes

### MainActivity

Type: Activity / orchestration class

Responsibilities:
- application entry screen
- initializes map, weather, report, and polling controllers
- binds XML views from `activity_main.xml`
- handles permissions, location updates, activity results, and button clicks
- loads reserves from backend
- loads published events for reserves
- submits traveler reports
- refreshes map and summary UI

Key owned fields:
- `ExecutorService executorService`
- `List<Reserve> reserves`
- `List<Event> allHazards`
- `List<Uri> selectedMediaUris`
- `ReserveNetworkRepository reserveRepository`
- `WeatherRepository weatherRepository`
- `MapToggleUiController toggleUiController`
- `MapController mapController`
- `WeatherUiController weatherController`
- `EventPollingController hazardPollingController`
- `EventReportUiController reportUiHelper`

Important operations:
- `onCreate`
- `onMapReady`
- `loadReserves`
- `loadPublishedHazards`
- `submitReport`
- `refreshMap`
- `refreshReserveSummary`
- location permission and location update handlers

### MapController

Type: UI/controller class

Responsibilities:
- owns Google Map drawing logic
- renders reserve boundaries
- renders reserve labels/markers
- renders event markers
- switches map style / POI visibility state
- centers and animates camera

Important operations:
- initialize map instance
- refresh map overlays
- draw reserve polygon
- draw reserve marker
- draw event marker
- apply map style

### WeatherUiController

Type: UI/controller class

Responsibilities:
- manages weather overlay state
- requests weather data on background executor
- stores current weather and hourly forecast for the active state
- renders weather text into the overlay views
- shows fallback messages when weather is unavailable

Key owned fields:
- `Activity activity`
- `WeatherRepository weatherRepository`
- `ExecutorService executorService`
- `WeatherCurrent currentWeather`
- `List<WeatherHourlyForecast> currentHourly`

Important operations:
- refresh weather
- render current weather
- render hourly forecast
- clear or show fallback state

### EventPollingController

Type: controller / scheduler class

Responsibilities:
- schedules periodic hazard/event polling
- uses main-thread `Handler` and `Looper`
- runs polling only when allowed by a condition callback

Key owned fields:
- `long pollIntervalMs`
- `BooleanSupplier shouldPollNow`
- `Runnable pollAction`
- `Handler handler`

Important operations:
- start polling
- stop polling
- schedule next poll

### EventReportUiController

Type: UI/controller class

Responsibilities:
- manages the event report panel visibility
- manages manual report location mode on the map
- adds or removes the temporary manual report marker
- updates location text for the report panel

Key owned fields:
- manual report marker reference
- panel visibility state
- manual location mode state

Important operations:
- toggle report panel
- enable manual location mode
- handle chosen manual location
- clear manual marker
- update displayed report location

### MapToggleUiController

Type: small UI helper/controller

Responsibilities:
- updates visual state of map-related toggle buttons
- reflects whether layers or overlays are active

Important operations:
- apply selected/unselected button styling

### ReserveNetworkRepository

Type: repository / network access class

Responsibilities:
- fetches reserves from backend API
- fetches published events for reserves from backend API
- uploads traveler reports as multipart form data
- parses JSON returned from backend into app models

Important operations:
- `loadReserves`
- `loadPublishedHazards`
- `submitTravelerReport`
- internal JSON parsing helpers

### WeatherRepository

Type: repository / network access class

Responsibilities:
- fetches current weather from OpenWeather API
- fetches hourly forecast from OpenWeather API
- parses JSON into weather model objects

Important operations:
- `loadCurrentWeather`
- `loadHourlyForecast`
- internal HTTP/JSON helper methods

### ReserveUtils

Type: utility class

Responsibilities:
- finds which reserve contains or is closest to a given location
- counts visible events for a reserve
- performs geography-related helper calculations used by the UI

Important operations:
- determine current reserve from `LatLng`
- count visible hazards/events for a reserve

### HttpUtils

Type: utility class

Responsibilities:
- reads HTTP response bodies from a `HttpURLConnection`
- throws when responses are unsuccessful

### ApiConfig

Type: configuration class

Responsibilities:
- exposes backend base URL
- exposes OpenWeather API key
- bridges build config values into Java code

## Model Classes

### Reserve

Type: domain model

Represents:
- one nature reserve returned from backend

Main data:
- reserve id
- reserve name
- center latitude/longitude or equivalent display coordinates
- `AreaBounds` boundary

### AreaBounds

Type: value object

Represents:
- rectangular geographic bounds of a reserve

Main data:
- min latitude
- max latitude
- min longitude
- max longitude

### Event

Type: domain model

Represents:
- one published event / hazard associated with a reserve

Main data:
- event id
- reserve id
- event type
- description
- latitude/longitude
- optional timestamp or severity-related display fields if present

### TravelerReportData

Type: request payload model

Represents:
- data collected from the report form before upload

Main data:
- reporter name
- selected reserve
- report type
- description
- report latitude/longitude
- selected media URIs

### WeatherCurrent

Type: domain model

Represents:
- current weather snapshot

Main data:
- temperature
- description
- wind / humidity / icon or equivalent current conditions fields

### WeatherHourlyForecast

Type: domain model

Represents:
- one hourly forecast entry

Main data:
- forecast time
- temperature
- description / icon

## Relationships

Use these as UML associations/dependencies:

- `MainActivity` uses `MapController`
- `MainActivity` uses `WeatherUiController`
- `MainActivity` uses `EventPollingController`
- `MainActivity` uses `EventReportUiController`
- `MainActivity` uses `MapToggleUiController`
- `MainActivity` uses `ReserveNetworkRepository`
- `MainActivity` uses `WeatherRepository`
- `MainActivity` uses `ReserveUtils`
- `MainActivity` creates `TravelerReportData`
- `MainActivity` owns many `Reserve`
- `MainActivity` owns many `Event`

- `WeatherUiController` uses `WeatherRepository`
- `WeatherUiController` uses shared `ExecutorService`
- `WeatherUiController` stores one `WeatherCurrent`
- `WeatherUiController` stores many `WeatherHourlyForecast`

- `EventPollingController` depends on `BooleanSupplier`
- `EventPollingController` depends on `Runnable`
- `EventPollingController` uses Android `Handler`
- `EventPollingController` uses Android `Looper`

- `EventReportUiController` depends on Google `GoogleMap`
- `EventReportUiController` creates and manages a temporary Google Maps `Marker`

- `MapToggleUiController` supports `MainActivity`
- `MapToggleUiController` reflects `MapController` state

- `MapController` depends on Google `GoogleMap`
- `MapController` reads many `Reserve`
- `MapController` reads `AreaBounds`
- `MapController` reads many `Event`

- `ReserveNetworkRepository` uses `ApiConfig`
- `ReserveNetworkRepository` uses `HttpUtils`
- `ReserveNetworkRepository` creates many `Reserve`
- `ReserveNetworkRepository` creates `AreaBounds`
- `ReserveNetworkRepository` creates many `Event`
- `ReserveNetworkRepository` consumes `TravelerReportData`

- `WeatherRepository` uses `ApiConfig`
- `WeatherRepository` uses `HttpUtils`
- `WeatherRepository` creates `WeatherCurrent`
- `WeatherRepository` creates many `WeatherHourlyForecast`

- `ReserveUtils` reads `Reserve`
- `ReserveUtils` reads `AreaBounds`
- `ReserveUtils` reads `Event`

- `Reserve` has one `AreaBounds`
- `Event` belongs to one `Reserve` through `reserveId`

## Suggested UML Cardinalities

Use these multiplicities when drawing:

- `MainActivity` -> `MapController` : `1..1`
- `MainActivity` -> `WeatherUiController` : `1..1`
- `MainActivity` -> `EventPollingController` : `1..1`
- `MainActivity` -> `EventReportUiController` : `1..1`
- `MainActivity` -> `MapToggleUiController` : `1..1`
- `MainActivity` -> `ReserveNetworkRepository` : `1..1`
- `MainActivity` -> `WeatherRepository` : `1..1`
- `MainActivity` -> `Reserve` : `1..*`
- `MainActivity` -> `Event` : `0..*`

- `Reserve` -> `AreaBounds` : `1..1`
- `Reserve` -> `Event` : `0..*`

- `WeatherUiController` -> `WeatherCurrent` : `0..1`
- `WeatherUiController` -> `WeatherHourlyForecast` : `0..*`

- `ReserveNetworkRepository` -> `Reserve` : `0..*`
- `ReserveNetworkRepository` -> `Event` : `0..*`
- `WeatherRepository` -> `WeatherHourlyForecast` : `0..*`

## External Systems

Model these as external actors or packages, not internal classes:

- Android Framework
- Google Maps SDK
- Google Play Services Location
- Backend REST API
- OpenWeather REST API
- Android XML resources in `res/layout`, `res/values`, `res/xml`, and `AndroidManifest.xml`

## Resource / XML Notes

If the UML tool supports notes, attach these notes:

- `MainActivity` binds most widgets from `activity_main.xml`
- the report panel UI is defined in `activity_main.xml`
- the weather overlay UI is defined in `activity_main.xml`
- app permissions, launcher activity, `FileProvider`, and Maps API key metadata are defined in `AndroidManifest.xml`
- map styling JSON files in `res/raw` or equivalent resources are used by `MapController`

## Optional Runtime Notes

If the tool also supports a component or sequence diagram, include these runtime notes:

- UI work runs on the Android main thread
- network work runs on a single background `ExecutorService`
- periodic event polling is scheduled by `EventPollingController` on the main looper
- repositories perform synchronous HTTP work and rely on callers to run them off the UI thread
