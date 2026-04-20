# Mobile App File Reference Table

This document summarizes the Java classes and XML files in the Android `mobile-app` module.

| File name | Description and role |
| --- | --- |
| `MainActivity.java` | Main Android activity and screen coordinator. It binds the layout, creates the helper controllers and API clients, owns shared UI state, and drives map, location, weather, hazard loading, and report submission flows. |
| `MapController.java` | Owns Google Map rendering. It draws reserve boundaries, hazard markers, POI markers, applies the active map style, and resizes custom icons when zoom changes. |
| `MapToggleUiController.java` | Updates the labels, colors, and visual states of the POI, hazard, and weather toggle controls. |
| `LocationController.java` | Manages location permission flow, last-known location lookup, and live GPS updates through `FusedLocationProviderClient`. |
| `ReserveStateResolver.java` | Resolves whether the traveler is inside a reserve, outside all reserves, or still missing location, and calculates hazard counts plus fire-alert state for the active reserve. |
| `WeatherUiController.java` | Controls the weather overlay UI, handles expand/collapse state, caches recent weather data, and decides when weather should be reloaded. |
| `WeatherApiClient.java` | Calls the OpenWeather API and maps current-weather and hourly-forecast JSON responses into app model objects. |
| `ReportApiClient.java` | Uploads traveler reports and optional media attachments to the backend using multipart HTTP requests. |
| `EventPollingController.java` | Repeats hazard refresh on a timer so the traveler-facing map stays updated without manual reloads. |
| `EventReportUiController.java` | Manages report panel visibility, manual map-point selection, report-location text, and the temporary marker for a selected report point. |
| `ReportMediaController.java` | Handles media picking, camera photo capture setup, attachment state, and the attachment-count text shown in the report form. |
| `ReportSubmissionController.java` | Validates report input, resolves the final report coordinates, builds `TravelerReportData`, submits the report, and reports success or failure back to the activity. |
| `ReserveApiClient.java` | Calls the backend public API to load reserves, traveler-visible hazards, and reserve POIs, then maps the JSON responses into model objects. |
| `Reserve.java` | Data model for one reserve, including its ID, display name, center point, and area bounds. |
| `AreaBounds.java` | Rectangle model for reserve boundary coordinates and basic point-inside-area checks. |
| `Event.java` | Data model for one traveler-visible hazard or event shown on the map, including type, priority, description, and coordinates. |
| `Poi.java` | Data model for one public point of interest displayed on the map. |
| `ReserveState.java` | Compact UI-facing state object that represents no-location, inside-reserve, or outside-reserve status and stores hazard-summary details. |
| `TravelerReportData.java` | Data model for the traveler report payload before it is uploaded to the backend. |
| `WeatherCurrent.java` | Data model for the current weather snapshot shown in the weather overlay. |
| `WeatherHourlyForecast.java` | Data model for one hourly forecast item shown in the expanded weather panel. |
| `AndroidManifest.xml` | Declares app permissions, the launcher `MainActivity`, the Google Maps API key metadata, and the `FileProvider` used by camera capture. |
| `activity_main.xml` | Main screen layout. It defines the map fragment, top status card, floating controls, weather overlay, side drawer, and report panel. |
| `strings.xml` | Central string resource file for user-facing text, status messages, report labels, string arrays, and pluralized attachment and hazard text. |
| `dimens.xml` | Shared dimensions for padding, floating controls, panel sizes, drawer width, and weather overlay placement across all screen sizes and orientations. |
| `file_paths.xml` | `FileProvider` path configuration that allows camera-created report images to be shared safely through app-managed file paths. |
| `bg_map_round_button.xml` | Base circular drawable background used by the floating map buttons. |
| `bg_map_round_button_active.xml` | Active-state circular background used when the weather toggle is on. |
| `bg_map_round_button_inactive.xml` | Inactive-state circular background used when the weather toggle is off. |
| `bg_map_round_button_alert.xml` | Alert-state circular background used for the menu button when the current reserve has an active fire hazard. |
| `ic_menu_hamburger.xml` | Vector icon used by the floating menu button. |
| `ic_weather_sun_cloud.xml` | Vector icon used by the weather toggle button. |

## Notes

- This table covers the mobile app Java classes and XML files only.
- PNG marker icons and raw JSON map-style files are part of the mobile app too, but they are outside the scope of this table.
