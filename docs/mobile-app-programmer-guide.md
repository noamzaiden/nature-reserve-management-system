# Mobile App Programmer Guide

This guide is for a programmer who wants to understand, maintain, or extend the Android `mobile-app` module.

Related docs:

- [Mobile App Architecture and Planning Document](./mobile-app-planning.md)
- [Android App Block Diagram](./android-app-block-diagram.md)
- [MainActivity Workflow](./main-activity-workflow.md)
- [Mobile App UML Class Diagram](./mobile-app-uml-class-diagram.md)

## Fast Reading Order

If you want the fastest useful mental model, read the app in this order:

1. `mobile-app/README.md`
2. `mobile-app/app/build.gradle`
3. `mobile-app/app/src/main/AndroidManifest.xml`
4. `mobile-app/app/src/main/res/layout/activity_main.xml`
5. `mobile-app/app/src/main/java/com/reserve/mobile/MainActivity.java`
6. `MapController.java`
7. `LocationController.java`
8. `ReserveStateResolver.java`
9. `WeatherUiController.java`
10. `ReportSubmissionController.java`
11. `ReserveApiClient.java`
12. `ReportApiClient.java`
13. `WeatherApiClient.java`
14. model classes

## Architecture In One Page

The app is a single-screen Android traveler client.

- `MainActivity` is the screen coordinator.
- Helper controllers keep map, weather, location, report UI, report submission, and polling logic focused.
- `ReserveApiClient`, `ReportApiClient`, and `WeatherApiClient` are the network-facing classes.
- Simple model classes carry mapped data.
- Resources provide layout, strings, icons, map styles, and local `FileProvider` paths.

There is no MVVM, no fragment navigation, and no repository abstraction layer beyond the three API client classes.

## Core Runtime Workflows

### Startup workflow

1. `MainActivity.onCreate(...)` runs.
2. Views are bound.
3. Controllers and API clients are created.
4. Button listeners and activity-result launchers are registered.
5. Reserve loading starts on the executor.
6. The map fragment later triggers `onMapReady(...)`.
7. Once the map is ready, location tracking starts.

### Reserve and hazard workflow

1. `loadReserves()` calls `ReserveApiClient.loadReserves()`.
2. `onReservesLoaded(...)` updates the spinner and immediately requests hazards and POIs.
3. `loadPublishedHazards()` calls `ReserveApiClient.loadPublishedHazards(...)`.
4. `onHazardsLoaded(...)` stores hazards, refreshes the map, and starts polling.
5. `EventPollingController` repeats the hazard refresh every 15 seconds.

### Location workflow

1. `LocationController.startTracking(...)` checks permission.
2. If needed, it asks the activity to request permissions.
3. Once allowed, it enables the my-location layer, asks for last known location, and subscribes to live updates.
4. `MainActivity.applyCurrentLocation(...)` stores the latest location and updates reserve state.

### Weather workflow

1. Traveler toggles weather on.
2. `WeatherUiController.refreshWeather(...)` decides whether cached data is still valid.
3. If not, it uses `WeatherApiClient` to fetch current and hourly weather.
4. The overlay text updates on the UI thread.

### Report workflow

1. Traveler opens the report panel.
2. `ReportMediaController` handles attachments and camera photos.
3. `EventReportUiController` handles manual map-point selection.
4. `ReportSubmissionController.submitReport(...)` validates the form and resolves coordinates.
5. `ReportApiClient.submitTravelerReport(...)` uploads the report.
6. On success, the form resets and hazards reload.

## App Structure

### Presentation

- `MainActivity`
- `activity_main.xml`
- strings, colors, and dimensions in resources

### Workflow helpers

- `MapController`
- `MapToggleUiController`
- `LocationController`
- `ReserveStateResolver`
- `WeatherUiController`
- `EventPollingController`
- `EventReportUiController`
- `ReportMediaController`
- `ReportSubmissionController`

### Integration

- `ReserveApiClient`
- `ReportApiClient`
- `WeatherApiClient`

### Models

- `Reserve`
- `AreaBounds`
- `Event`
- `Poi`
- `ReserveState`
- `TravelerReportData`
- `WeatherCurrent`
- `WeatherHourlyForecast`

## Method-By-Method Reference

This section explains what each method in the Java source files does.

### `MainActivity`

Role:
Single screen controller and state owner.

Callback adapters inside the file:

- `reportSubmissionHost.setBusyState(...)`
  Lets `ReportSubmissionController` enable or disable interactive controls and update the status line.
- `reportSubmissionHost.requestLocationTracking()`
  Reuses the activity location-tracking flow if report submission needs location permission.
- `reportSubmissionHost.onReportLocationResolved(...)`
  Feeds resolved report coordinates back into the activity state.
- `reportSubmissionHost.onMissingLocationForReport()`
  Refreshes the report location text when a fresh GPS fix could not be obtained.
- `reportSubmissionHost.onReportSubmitted()`
  Clears the draft and closes the report panel.
- `reportSubmissionHost.reloadHazards()`
  Refreshes traveler-visible hazards after a successful report.
- `reportSubmissionHost.updateServerStatus(...)`
  Updates the drawer server badge after report submission.

- `locationControllerHost.hasLocationPermission()`
  Lets `LocationController` ask the activity whether location permission is already granted.
- `locationControllerHost.requestLocationPermissions()`
  Starts the Android permission request launcher.
- `locationControllerHost.onLocationPermissionDenied()`
  Shows the location-permission message and recomputes the reserve-state UI.
- `locationControllerHost.onInitialLocationAvailable(...)`
  Applies a last-known location without animation.
- `locationControllerHost.onLiveLocationAvailable(...)`
  Applies a live GPS update and recenters the map if follow mode is still enabled.
- `locationControllerHost.onLocationUnavailable()`
  Shows the waiting-for-location state and refreshes the weather overlay.

Methods:

- `onCreate(...)`
  Builds controllers and API clients, binds the layout, wires UI behavior, initializes visible text, and starts reserve loading.
- `bindViews()`
  Caches every important `View` from `activity_main.xml`.
- `configureTypeSpinner()`
  Loads report types from `strings.xml` into the report-type spinner.
- `configureMediaPicker()`
  Registers the document picker and camera capture activity-result launchers.
- `configureLocationPermissionLauncher()`
  Registers the location permission launcher and forwards the result to `LocationController`.
- `configureButtons()`
  Connects every visible button in the activity to its action.
- `configureMap()`
  Finds the `SupportMapFragment` and requests the async map callback.
- `onMapReady(...)`
  Stores the `GoogleMap`, disables unused default controls, wires camera and tap listeners, attaches `MapController`, and starts location tracking.
- `loadReserves()`
  Runs the first backend reserve fetch on the executor.
- `loadPublishedHazards()`
  Loads traveler-visible hazards for all reserves unless a refresh is already running.
- `loadReservePois()`
  Loads public POIs for all reserves.
- `toggleReportPanel()`
  Opens or closes the report card and warns the user if no active reserve is currently matched.
- `startManualLocationSelection()`
  Puts the report flow into map-tap mode.
- `handleMapClickForManualLocation(...)`
  Stores the tapped map point as the manual report location when selection mode is active.
- `toggleWeather()`
  Enables or disables the weather overlay and refreshes weather state.
- `refreshMapContent()`
  Delegates a full reserve/POI/hazard redraw to `MapController`.
- `onMapLayerChanged(...)`
  Refreshes map content and toggle labels after POI or hazard toggle changes, optionally also recomputing reserve-state UI.
- `onReservesLoaded(...)`
  Replaces the reserve list, repopulates the spinner, refreshes the map, and starts hazard and POI loading.
- `onPoisLoaded(...)`
  Stores new POI data and redraws the map.
- `onHazardsLoaded(...)`
  Stores new hazard data, redraws the map, refreshes reserve state, clears the status line, and starts polling.
- `updateToggleLabels()`
  Delegates toggle-button text and tint updates to `MapToggleUiController`.
- `updateReserveState()`
  Computes `ReserveState` from the latest location and data, then updates location hint text, hazard count text, and follow-up UI refreshes.
- `renderNoLocationState(...)`
  Renders the screen when no usable location is available yet.
- `renderInsideReserveState(...)`
  Renders the screen when the traveler is inside a known reserve.
- `renderOutsideReserveState()`
  Renders the screen when the traveler is outside every known reserve.
- `finalizeReserveStateRefresh()`
  Runs the shared refresh steps that always follow a reserve-state change.
- `hazardCountTextForState(...)`
  Builds the correct pluralized hazard-count string for the current state.
- `selectReserveInSpinner(...)`
  Keeps the reserve spinner aligned with the reserve detected from location.
- `startLocationTracking()`
  Starts `LocationController` if it is available.
- `applyCurrentLocation(...)`
  Stores the latest location, recenters if requested, and recalculates reserve state.
- `moveCameraToUser(...)`
  Moves or animates the map camera to the current user location.
- `resetMapOrientation()`
  Returns the map camera to north-up, flat orientation.
- `launchCameraCapture()`
  Asks `ReportMediaController` for a `Uri` and launches the camera contract.
- `submitTravelerReport()`
  Collects the current form state and hands it to `ReportSubmissionController`.
- `resetReportDraft()`
  Clears text input, attachments, and manual report-location state after successful submission.
- `setBusyState(...)`
  Updates the status line and disables or enables interactive controls during background work.
- `updateServerStatus(...)`
  Sets the drawer badge to checking, online, or offline.
- `showShortToast(...)`
  Convenience wrapper for short user messages.
- `refreshWeather(...)`
  Delegates weather refresh to `WeatherUiController`.
- `updateReportLocationText()`
  Delegates report-location label rendering to `EventReportUiController`.
- `getSelectedReserve()`
  Returns the currently selected reserve from the spinner, or `null` if the spinner does not point to a valid item.
- `hasLocationPermission()`
  Checks whether fine or coarse location permission is currently granted.
- `onDestroy()`
  Stops polling, stops GPS tracking, and shuts down the executor.

### `MapController`

Role:
Owns Google Map drawing behavior.

Methods:

- `MapController(...)`
  Stores the Android `Context` used for resources.
- `attachMap(...)`
  Stores the `GoogleMap`, moves it to the default camera position, and applies the initial map style.
- `isShowingPois()`
  Returns whether POI markers are currently enabled.
- `setShowPois(...)`
  Updates POI visibility and reapplies the map style.
- `isShowingHazards()`
  Returns whether hazard markers are enabled.
- `setShowHazards(...)`
  Updates hazard visibility.
- `refresh(...)`
  Clears the map and redraws reserve polygons, POIs, and hazards. Also enables the my-location layer if permission exists.
- `drawReserveBoundsForAll(...)`
  Iterates over every reserve and draws its polygon.
- `drawHazardsIfEnabled(...)`
  Draws hazard markers only when the hazard layer is active.
- `drawPoisIfEnabled(...)`
  Draws POI markers only when the POI layer is active.
- `drawReserveBounds(...)`
  Draws one rectangular reserve polygon and highlights the active reserve.
- `drawHazardMarker(...)`
  Draws one hazard marker with a priority-based Google Maps hue.
- `drawPoiMarker(...)`
  Draws one POI marker using a typed custom icon when available.
- `applyMapStyle()`
  Switches between the two raw map-style JSON files depending on POI visibility.
- `priorityHue(...)`
  Maps hazard priority strings to Google marker colors.
- `poiIconDescriptor(...)`
  Returns a cached or newly created `BitmapDescriptor` for a POI type.
- `poiIconResourceId(...)`
  Maps normalized POI type names and aliases to drawable resource IDs.
- `createPoiIconDescriptor(...)`
  Renders a drawable resource into a bitmap so both PNG and vector POI icons can be used on the map.
- `normalizePoiType(...)`
  Trims and lowercases a POI type for lookup.

### `MapToggleUiController`

Role:
Owns the button visuals for layer toggles.

Methods:

- `updateLabels(...)`
  Sets POI and hazard button text and updates all toggle button styles.
- `applyToggleStyle(...)`
  Applies fill and stroke colors to one Material button.
- `updateWeatherToggleIcon(...)`
  Switches the weather button background, tint, alpha, and content description.

### `LocationController`

Role:
Owns the GPS tracking flow.

Host callbacks:

- `hasLocationPermission()`
  Host-side permission check.
- `requestLocationPermissions()`
  Host-side permission request trigger.
- `onLocationPermissionDenied()`
  Host notification that the user denied location permission.
- `onInitialLocationAvailable(...)`
  Host notification for last-known location.
- `onLiveLocationAvailable(...)`
  Host notification for continuous location updates.
- `onLocationUnavailable()`
  Host notification that last-known location was unavailable.

Methods:

- `LocationController(...)`
  Stores the fused-location client.
- `onPermissionResult(...)`
  Resumes tracking on success or tells the host permission was denied.
- `startTracking(...)`
  Requests permission if missing, enables the my-location layer, asks for last-known location, and starts live updates.
- `stopTracking()`
  Removes live-location updates and marks tracking as stopped.
- `enableMyLocationLayer(...)`
  Enables Google Maps’ built-in my-location layer when a map is present.
- `ensureLocationCallback(...)`
  Lazily creates the `LocationCallback` used for live updates.
- `requestLastKnownLocation(...)`
  Requests the cached last-known location for faster initial UI state.
- `buildLocationRequest()`
  Builds the high-accuracy location request with update intervals.
- `toLatLng(...)`
  Converts an Android `Location` to Google Maps `LatLng`.

### `ReserveStateResolver`

Role:
Computes reserve-state UI from raw data.

Methods:

- `resolve(...)`
  Returns `noLocation`, `insideReserve`, or `outsideReserve` based on current location and loaded data.
- `findReserveForLocation(...)`
  Finds the first reserve whose rectangular area contains the current location.
- `countVisibleHazards(...)`
  Counts hazards that have coordinates and optionally belong to one reserve.

### `WeatherUiController`

Role:
Owns weather UI state and caching behavior.

Methods:

- `WeatherUiController(...)`
  Stores the activity, weather service, and executor.
- `toggleExpanded()`
  Expands or collapses the hourly forecast panel.
- `refreshWeather(...)`
  Shows or hides the overlay, validates input state, decides whether weather must reload, and renders current/hourly weather.
- `showWeatherPanel(...)`
  Makes the weather card visible and sets the expand/collapse icon.
- `showStatus(...)`
  Writes loading, waiting, or unavailable status text into the weather views.
- `renderCurrentWeather(...)`
  Writes the compact current-weather line.
- `renderHourlyWeather(...)`
  Writes the expanded hourly forecast summary.
- `shouldReloadWeather(...)`
  Returns `true` when cached weather is stale by age or distance moved.

### `EventPollingController`

Role:
Repeats hazard reload on a timer.

Methods:

- `EventPollingController(...)`
  Stores the interval and action to repeat.
- `start()`
  Starts the repeating timer unless it is already running.
- `stop()`
  Stops the timer and removes pending callbacks.

### `EventReportUiController`

Role:
Owns report-panel visibility and manual map-point state.

Methods:

- `setReportPanelVisible(...)`
  Shows or hides the report panel and updates the toggle button label.
- `toggleReportPanel(...)`
  Flips report-panel visibility and returns the new state.
- `startManualLocationSelection(...)`
  Puts the report flow into map-tap mode and updates the button label.
- `isSelectingManualLocation()`
  Returns whether the next map tap should become a report location.
- `saveManualLocation(...)`
  Stores the tapped location, creates or moves the manual marker, resets the button text, and refreshes the report-location label.
- `getManualReportLatLng()`
  Returns the currently selected manual report point, if any.
- `clearManualLocation(...)`
  Removes manual report location and marker state and restores the default button label.
- `updateReportLocationText(...)`
  Shows the manual report point when present, otherwise the current GPS point, otherwise the waiting message.

### `ReportMediaController`

Role:
Owns attachment-selection and camera-photo state.

Methods:

- `ReportMediaController(...)`
  Stores the activity and the selected-media text view.
- `openMediaPicker(...)`
  Launches the document picker for images and videos.
- `onMediaPicked(...)`
  Adds selected `Uri`s to the current attachment list and refreshes the attachment count text.
- `prepareCameraCapture()`
  Creates and stores the pending camera-output `Uri`.
- `onCameraCaptureResult(...)`
  Adds the captured image to attachments on success, or shows a cancellation message on failure.
- `clearSelectedMedia()`
  Clears attachments and pending camera state.
- `getSelectedMediaUris()`
  Returns a read-only view of the selected attachment list.
- `updateSelectedMediaText()`
  Writes either “no attachments” or a pluralized attachment count.
- `createCameraImageUri()`
  Creates the `reports` image directory under external pictures storage and returns a `FileProvider` `Uri`.
- `showShortToast(...)`
  Shows a short message from inside the controller.

### `ReportSubmissionController`

Role:
Owns report validation, fresh location resolution, and upload flow.

Host callbacks:

- `setBusyState(...)`
  Lets the host disable controls and update status text.
- `requestLocationTracking()`
  Lets the host resume permission or location flow when report submission needs it.
- `onReportLocationResolved(...)`
  Sends the final coordinates back to the host.
- `onMissingLocationForReport()`
  Tells the host a fresh GPS fix could not be obtained.
- `onReportSubmitted()`
  Tells the host the upload succeeded and cleanup can run.
- `reloadHazards()`
  Tells the host to reload traveler-visible hazards.
- `updateServerStatus(...)`
  Lets the host badge backend availability.

Methods:

- `ReportSubmissionController(...)`
  Stores Android dependencies, executor, and `ReportApiClient`.
- `submitReport(...)`
  Validates the form, prefers a manual map point when one exists, otherwise requests a fresh GPS fix, then starts upload.
- `uploadReport(...)`
  Builds `TravelerReportData`, uploads it on the executor, and reports success or failure back to the host.
- `handleMissingLocation(...)`
  Restores non-busy UI state and informs the host and user that location is still unavailable.
- `safeReportType(...)`
  Normalizes a missing or blank report type to `OTHER`.
- `showShortToast(...)`
  Shows a short user message.
- `showLongToast(...)`
  Shows a long success message.

### `ReserveApiClient`

Role:
Public backend client for reserves, hazards, and POIs.

Methods:

- `loadReserves()`
  Loads and maps the public reserve list.
- `loadPublishedHazards(...)`
  Loads traveler-visible hazards for every reserve and combines the results.
- `loadPois(...)`
  Loads public POIs for every reserve and combines the results.
- `parseReserve(...)`
  Maps one reserve JSON object into a `Reserve`.
- `parseReserves(...)`
  Maps a JSON array into a `List<Reserve>`.
- `parseHazardsForReserve(...)`
  Loads and maps all hazards for one reserve.
- `parsePoisForReserve(...)`
  Loads and maps all POIs for one reserve.
- `parseAreaBounds(...)`
  Maps a JSON `area` object into `AreaBounds`.
- `parseEvent(...)`
  Maps one event JSON object into an `Event`.
- `parsePoi(...)`
  Maps one POI JSON object into a `Poi`.
- `loadJsonArray(...)`
  Loads a backend path and parses the response as `JSONArray`.
- `readJsonFromGet(...)`
  Opens a GET request, validates the status code, and returns the raw response string.
- `openJsonGetConnection(...)`
  Builds a GET `HttpURLConnection`.
- `requireSuccessResponse(...)`
  Throws if the response status is not 2xx.
- `readResponseText(...)`
  Reads the response stream into a UTF-8 string.

### `ReportApiClient`

Role:
Backend client for traveler report upload.

Methods:

- `submitTravelerReport(...)`
  Uploads one traveler report and its optional attachments as multipart form data.
- `writeFormField(...)`
  Writes one plain text multipart form field.
- `writeFileField(...)`
  Writes one attachment body from a `Uri`.
- `openMultipartPostConnection(...)`
  Builds a multipart POST `HttpURLConnection`.
- `requireSuccessResponse(...)`
  Throws if the response status is not 2xx.

### `WeatherApiClient`

Role:
Weather API client and JSON mapper.

Methods:

- `hasApiKey()`
  Returns whether a usable weather API key is configured.
- `loadCurrentWeather(...)`
  Loads and maps the current weather snapshot.
- `loadHourlyWeather(...)`
  Loads and maps a bounded list of hourly forecast entries.
- `buildWeatherUrl(...)`
  Builds the current-weather URL.
- `buildForecastUrl(...)`
  Builds the forecast URL by transforming the configured current-weather endpoint when needed.
- `buildQueryParameters(...)`
  Builds the shared weather query string.
- `requireApiKey()`
  Throws if the weather API key is missing.
- `parseCondition(...)`
  Extracts the first weather condition description or fallback label.
- `readTemperature(...)`
  Extracts the `temp` field from a JSON `main` object.
- `buildHourlyInfo(...)`
  Maps one forecast JSON item into `WeatherHourlyForecast`.
- `readJson(...)`
  Loads one JSON object from a URL and validates the response.
- `openJsonGetConnection(...)`
  Builds a GET `HttpURLConnection`.
- `requireSuccessResponse(...)`
  Throws if the response status is not 2xx.
- `readResponseText(...)`
  Reads a response stream into a UTF-8 string.
- `extractHourLabel(...)`
  Converts `dt_txt` into an `HH:mm` label or falls back to `Soon`.
- `capitalizeWords(...)`
  Normalizes a condition string for display.

### `ReserveState`

Role:
Compact state object used by the activity.

Methods:

- `noLocation(...)`
  Factory for the “no location yet” state.
- `insideReserve(...)`
  Factory for the “inside a reserve” state.
- `outsideReserve(...)`
  Factory for the “outside all reserves” state.
- `getKind()`
  Returns the enum describing which state variant this is.
- `getActiveReserve()`
  Returns the matched reserve when the state is inside a reserve.
- `getVisibleHazardCount()`
  Returns the hazard count that matches this state.
- `hasActiveReserve()`
  Convenience check for whether a reserve is attached.
- `isNoLocation()`
  Convenience check for the “no location” state.

### `Reserve`

Methods:

- `Reserve(...)`
  Builds a reserve and falls back from `displayName` to `fallbackName` when needed.
- `getId()`
  Returns the reserve ID.
- `getDisplayName()`
  Returns the display label shown in the UI.
- `getCenterLatitude()`
  Returns the reserve center latitude.
- `getCenterLongitude()`
  Returns the reserve center longitude.
- `getAreaBounds()`
  Returns the rectangular bounds.
- `hasCenterPoint()`
  Returns whether the reserve has usable center coordinates.
- `toString()`
  Returns the display name so spinner items show meaningful text.

### `AreaBounds`

Methods:

- `AreaBounds(...)`
  Stores min/max latitude and longitude.
- `getMinLatitude()`
  Returns the southern edge.
- `getMaxLatitude()`
  Returns the northern edge.
- `getMinLongitude()`
  Returns the western edge.
- `getMaxLongitude()`
  Returns the eastern edge.
- `contains(...)`
  Returns whether a point lies inside the rectangle.

### `Event`

Methods:

- `Event(...)`
  Stores one hazard record.
- `getReserveId()`
  Returns the owning reserve ID.
- `getType()`
  Returns the event type.
- `getPriority()`
  Returns the event priority.
- `getDescription()`
  Returns the traveler-facing description.
- `hasCoordinates()`
  Returns whether the event has usable coordinates.
- `latLng()`
  Converts stored coordinates into `LatLng`.

### `Poi`

Methods:

- `Poi(...)`
  Stores one POI record and normalizes missing type or name.
- `getReserveId()`
  Returns the owning reserve ID.
- `getType()`
  Returns the POI type.
- `getName()`
  Returns the display name.
- `getDescription()`
  Returns the description.
- `hasCoordinates()`
  Returns whether the POI has usable coordinates.
- `latLng()`
  Converts stored coordinates into `LatLng`.

### `TravelerReportData`

Methods:

- `TravelerReportData(...)`
  Stores report metadata and snapshots attachment `Uri`s into a mutable copy.
- `getReserveId()`
  Returns the selected reserve ID.
- `getType()`
  Returns the report type.
- `getReporterName()`
  Returns the optional reporter name.
- `getDescription()`
  Returns the report description.
- `getLatitude()`
  Returns the report latitude.
- `getLongitude()`
  Returns the report longitude.
- `getAttachmentUris()`
  Returns the attachment list.

### `WeatherCurrent`

Methods:

- `WeatherCurrent(...)`
  Stores one current-weather snapshot.
- `getTemperatureCelsius()`
  Returns current temperature.
- `getCondition()`
  Returns the display condition string.

### `WeatherHourlyForecast`

Methods:

- `WeatherHourlyForecast(...)`
  Stores one hourly forecast item.
- `getHourLabel()`
  Returns the `HH:mm` style hour label.
- `getTemperatureCelsius()`
  Returns forecast temperature.
- `getCondition()`
  Returns forecast condition text.

## Resource Structure Notes

- `activity_main.xml`
  Defines the entire screen in one layout file.
- `strings.xml`
  Keeps traveler-facing copy out of Java code.
- `dimens.xml` families
  Adjust panel sizes and spacing by phone, landscape, and tablet-like screens.
- drawables
  Provide floating-button backgrounds, custom vector icons, and custom POI marker art.
- raw map styles
  Change the Google base map when app POIs are toggled.
- `file_paths.xml`
  Allows camera-created images to be shared through `FileProvider`.

## Best Places To Edit Common Features

- Change backend API handling:
  `ReserveApiClient.java`
- Change report upload handling:
  `ReportApiClient.java`
- Change weather parsing:
  `WeatherApiClient.java`
- Change weather overlay behavior:
  `WeatherUiController.java`
- Change reserve matching logic:
  `ReserveStateResolver.java`
- Change map markers or styles:
  `MapController.java`
- Change report validation or upload behavior:
  `ReportSubmissionController.java`
- Change report-panel interaction:
  `EventReportUiController.java` and `ReportMediaController.java`
- Change overall wiring:
  `MainActivity.java`

## Final Mental Model

If you remember one thing, remember this:

`MainActivity` coordinates, helper controllers focus workflows, API clients talk to the network, and model classes carry the data that flows between them.
