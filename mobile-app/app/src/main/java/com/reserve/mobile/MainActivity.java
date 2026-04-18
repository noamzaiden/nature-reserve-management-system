package com.reserve.mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final long HAZARD_POLL_INTERVAL_MS = 15000L;
    private static final int COLOR_STATUS_CHECKING = android.graphics.Color.parseColor("#735A2E");
    private static final int COLOR_STATUS_ONLINE = android.graphics.Color.parseColor("#2C7A57");
    private static final int COLOR_STATUS_OFFLINE = android.graphics.Color.parseColor("#B4473A");
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // UI fields
    private Spinner reserveSpinner;
    private Spinner reportTypeSpinner;
    private TextView locationHintText;
    private TextView statusText;
    private TextView eventCountText;
    private TextView weatherNowText;
    private TextView weatherHourlyText;
    private TextView selectedMediaText;
    private TextView reportLocationText;
    private TextView serverStatusText;
    private EditText reporterNameInput;
    private EditText reportDescriptionInput;
    private Button attachMediaButton;
    private Button capturePhotoButton;
    private Button submitReportButton;
    private MaterialButton manualLocationButton;
    private TextView northUpButton;
    private MaterialButton reportToggleButton;
    private MaterialButton poiToggleButton;
    private MaterialButton hazardToggleButton;
    private ImageButton weatherToggleButton;
    private ImageButton weatherExpandButton;
    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private ImageButton myLocationButton;
    private View reportPanel;
    private View bottomSpacer;
    private View weatherOverlay;
    private View weatherHourlyPanel;

    // Activity result launchers
    private ActivityResultLauncher<String[]> mediaPickerLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    // State fields
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<Reserve> reserves = new ArrayList<>();
    private final List<Event> allHazards = new ArrayList<>();
    private final ReserveService reserveService = new ReserveService();
    private final WeatherService weatherService = new WeatherService();
    private final ReserveStateResolver reserveStateResolver = new ReserveStateResolver();
    private final MapToggleUiController toggleUiController = new MapToggleUiController();
    private GoogleMap googleMap;
    private MapController mapController;
    private WeatherUiController weatherController;
    private EventPollingController hazardPollingController;
    private EventReportUiController reportUiHelper;
    private ReportMediaController reportMediaController;
    private ReportSubmissionController reportSubmissionController;
    private LocationController locationController;
    private LatLng currentUserLatLng;
    private ReserveState currentReserveState = ReserveState.noLocation(0);
    private boolean followUserCamera = true;
    private boolean hasCenteredOnUser = false;
    private boolean showWeather = false;
    private boolean hazardRefreshInFlight = false;

    // Controller hosts
    private final ReportSubmissionController.Host reportSubmissionHost = new ReportSubmissionController.Host() {
        @Override
        public void setBusyState(boolean busy, String message) {
            MainActivity.this.setBusyState(busy, message);
        }

        @Override
        public void requestLocationTracking() {
            startLocationTracking();
        }

        @Override
        public void onReportLocationResolved(LatLng latLng) {
            applyCurrentLocation(latLng, false, false);
        }

        @Override
        public void onMissingLocationForReport() {
            updateReportLocationText();
        }

        @Override
        public void onReportSubmitted() {
            clearReportForm();
            clearManualReportLocation();
            reportUiHelper.setReportPanelVisible(false, reportPanel, bottomSpacer, reportToggleButton);
        }

        @Override
        public void reloadHazards() {
            loadPublishedHazards();
        }

        @Override
        public void updateServerStatus(boolean online) {
            MainActivity.this.updateServerStatus(online);
        }
    };
    private final LocationController.Host locationControllerHost = new LocationController.Host() {
        @Override
        public boolean hasLocationPermission() {
            return MainActivity.this.hasLocationPermission();
        }

        @Override
        public void requestLocationPermissions() {
            locationPermissionLauncher.launch(LOCATION_PERMISSIONS);
        }

        @Override
        public void onLocationPermissionDenied() {
            setStatusText(getString(R.string.status_location_permission_needed));
            updateReserveState();
        }

        @Override
        public void onInitialLocationAvailable(LatLng latLng) {
            applyCurrentLocation(latLng, true, false);
        }

        @Override
        public void onLiveLocationAvailable(LatLng latLng) {
            boolean shouldCenterCamera = followUserCamera || !hasCenteredOnUser;
            applyCurrentLocation(latLng, shouldCenterCamera, hasCenteredOnUser);
        }

        @Override
        public void onLocationUnavailable() {
            setStatusText(getString(R.string.status_location_waiting));
            updateReportLocationText();
            refreshWeather(false);
        }
    };

    // Initializes UI state and starts the first data load.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Core helpers used by map/location and network tasks.
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapController = new MapController(this);
        weatherController = new WeatherUiController(this, weatherService, executorService);
        hazardPollingController = new EventPollingController(
                HAZARD_POLL_INTERVAL_MS,
                this::loadPublishedHazards
        );
        reportUiHelper = new EventReportUiController();
        locationController = new LocationController(fusedLocationClient);
        reportSubmissionController = new ReportSubmissionController(
                this,
                getContentResolver(),
                fusedLocationClient,
                executorService,
                reserveService
        );

        // Finish UI setup before the first network request starts.
        bindViews();
        reportMediaController = new ReportMediaController(this, selectedMediaText);
        configureTypeSpinner();
        configureMediaPicker();
        configureLocationPermissionLauncher();
        configureButtons();
        configureMap();
        updateToggleLabels();
        updateServerStatus(null);
        reportMediaController.updateSelectedMediaText();
        updateReportLocationText();
        setStatusText(getString(R.string.status_loading_reserves));
        loadReserves();
    }

    private void bindViews() {
        reserveSpinner = findViewById(R.id.reserve_spinner);
        reportTypeSpinner = findViewById(R.id.report_type_spinner);
        locationHintText = findViewById(R.id.location_hint_text);
        statusText = findViewById(R.id.status_text);
        eventCountText = findViewById(R.id.event_count_text);
        weatherNowText = findViewById(R.id.weather_now_text);
        weatherHourlyText = findViewById(R.id.weather_hourly_text);
        selectedMediaText = findViewById(R.id.selected_media_text);
        reportLocationText = findViewById(R.id.report_location_text);
        serverStatusText = findViewById(R.id.server_status_text);
        reporterNameInput = findViewById(R.id.reporter_name_input);
        reportDescriptionInput = findViewById(R.id.report_description_input);
        attachMediaButton = findViewById(R.id.attach_media_button);
        capturePhotoButton = findViewById(R.id.capture_photo_button);
        submitReportButton = findViewById(R.id.submit_report_button);
        manualLocationButton = findViewById(R.id.manual_location_button);
        northUpButton = findViewById(R.id.north_up_button);
        reportToggleButton = findViewById(R.id.report_toggle_button);
        poiToggleButton = findViewById(R.id.poi_toggle_button);
        hazardToggleButton = findViewById(R.id.hazard_toggle_button);
        weatherToggleButton = findViewById(R.id.weather_toggle_button);
        weatherExpandButton = findViewById(R.id.weather_expand_button);
        drawerLayout = findViewById(R.id.drawer_layout);
        menuButton = findViewById(R.id.menu_button);
        myLocationButton = findViewById(R.id.my_location_button);
        reportPanel = findViewById(R.id.report_panel);
        bottomSpacer = findViewById(R.id.bottom_spacer);
        weatherOverlay = findViewById(R.id.weather_overlay);
        weatherHourlyPanel = findViewById(R.id.weather_hourly_panel);
    }

    // Prepares the report type spinner options.
    private void configureTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.report_types)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(adapter);
    }

    // Registers media picker and camera activity result callbacks.
    private void configureMediaPicker() {
        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> reportMediaController.onMediaPicked(uris)
        );
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> reportMediaController.onCameraCaptureResult(Boolean.TRUE.equals(success))
        );
    }

    // Registers location permission callback handling.
    private void configureLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                            || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (locationController != null) {
                        locationController.onPermissionResult(granted, googleMap, locationControllerHost);
                    }
                }
        );
    }

    private void configureButtons() {
        configureReportButtons();
        configureDrawerAndMapButtons();
        configureLayerButtons();
    }

    // Sets up the event report panel buttons.
    private void configureReportButtons() {
        attachMediaButton.setOnClickListener(view -> reportMediaController.openMediaPicker(mediaPickerLauncher));
        capturePhotoButton.setOnClickListener(view -> launchCameraCapture());
        submitReportButton.setOnClickListener(view -> submitTravelerReport());
        manualLocationButton.setOnClickListener(view -> startManualLocationSelection());
        reportToggleButton.setOnClickListener(view -> toggleReportPanel());
    }

    // Connects menu, my-location, and north-up map control buttons.
    private void configureDrawerAndMapButtons() {
        menuButton.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));
        myLocationButton.setOnClickListener(view -> {
            followUserCamera = true;
            moveCameraToUser(true);
        });
        northUpButton.setOnClickListener(view -> resetMapOrientation());
    }

    // Connects map layer toggle controls (POI, hazards, weather).
    private void configureLayerButtons() {
        poiToggleButton.setOnClickListener(view -> {
            mapController.setShowPois(!mapController.isShowingPois());
            onMapLayerChanged(false);
        });
        hazardToggleButton.setOnClickListener(view -> {
            mapController.setShowHazards(!mapController.isShowingHazards());
            onMapLayerChanged(true);
        });
        weatherToggleButton.setOnClickListener(view -> toggleWeather());
        weatherExpandButton.setOnClickListener(view -> {
            weatherController.toggleExpanded();
            refreshWeather(false);
        });
    }

    // Gets map fragment and requests async map initialization.
    private void configureMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // Called when GoogleMap is ready; enables map features and tracking.
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        // We use custom controls, so disable default map chrome we do not need.
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                followUserCamera = false;
            }
        });
        googleMap.setOnMapClickListener(this::handleMapClickForManualLocation);
        mapController.attachMap(map);
        refreshMapContent();
        startLocationTracking();
        setStatusText("");
    }

    // Loads reserve list from backend and updates UI state.
    private void loadReserves() {
        executorService.execute(() -> {
            try {
                List<Reserve> loadedReserves = reserveService.loadReserves();
                runOnUiThread(() -> onReservesLoaded(loadedReserves));
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    updateServerStatus(false);
                    setStatusText(getString(R.string.status_reserve_load_failed));
                });
            }
        });
    }

    // Loads published hazards from backend and refreshes map/UI.
    private void loadPublishedHazards() {
        if (reserves.isEmpty() || hazardRefreshInFlight) {
            return;
        }
        hazardRefreshInFlight = true;
        executorService.execute(() -> {
            try {
                List<Event> loadedHazards = reserveService.loadPublishedHazards(reserves);
                runOnUiThread(() -> onHazardsLoaded(loadedHazards));
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    updateServerStatus(false);
                    setStatusText(getString(R.string.status_hazard_load_failed));
                    hazardRefreshInFlight = false;
                });
            }
        });
    }

    // Shows or hides the report panel and updates its toggle label.
    private void toggleReportPanel() {
        boolean reportPanelVisible = reportUiHelper.toggleReportPanel(reportPanel, bottomSpacer, reportToggleButton);
        if (reportPanelVisible && !currentReserveState.hasActiveReserve()) {
            showShortToast(R.string.report_pick_location);
        }
    }

    // Starts map-tap mode so user can manually place report coordinates.
    private void startManualLocationSelection() {
        reportUiHelper.startManualLocationSelection(manualLocationButton);
        showShortToast(R.string.report_manual_location_hint);
    }

    // Saves the map tap as manual report location when selection mode is active.
    private void handleMapClickForManualLocation(LatLng latLng) {
        if (!reportUiHelper.isSelectingManualLocation()) {
            return;
        }
        reportUiHelper.saveManualLocation(
                latLng,
                googleMap,
                manualLocationButton,
                reportLocationText,
                currentUserLatLng,
                this
        );
        showShortToast(R.string.report_manual_location_saved);
    }

    // Starts periodic hazard refresh so map stays updated automatically.
    private void startHazardPolling() {
        hazardPollingController.start();
    }

    // Stops periodic hazard refresh when screen is no longer active.
    private void stopHazardPolling() {
        hazardPollingController.stop();
    }

    // Toggles weather mode and refreshes weather UI/data.
    private void toggleWeather() {
        showWeather = !showWeather;
        updateToggleLabels();
        refreshWeather(showWeather);
    }

    // Redraws map layers using current data and toggle states.
    private void refreshMapContent() {
        mapController.refresh(reserves, allHazards, currentReserveState.getActiveReserve(), hasLocationPermission());
    }

    // Refreshes map and reserve state after one of the layer toggles changes.
    private void onMapLayerChanged(boolean updateReserveState) {
        refreshMapContent();
        updateToggleLabels();
        if (updateReserveState) {
            updateReserveState();
        }
    }

    // Applies reserve data to the spinner and starts hazard loading.
    private void onReservesLoaded(List<Reserve> loadedReserves) {
        updateServerStatus(true);
        reserves.clear();
        reserves.addAll(loadedReserves);
        ArrayAdapter<Reserve> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reserves);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reserveSpinner.setAdapter(adapter);
        refreshMapContent();
        updateReserveState();
        setStatusText(getString(R.string.status_loading_hazards));
        loadPublishedHazards();
    }

    // Replaces the hazard list with the newest backend data.
    private void onHazardsLoaded(List<Event> loadedHazards) {
        updateServerStatus(true);
        allHazards.clear();
        allHazards.addAll(loadedHazards);
        refreshMapContent();
        updateReserveState();
        setStatusText("");
        hazardRefreshInFlight = false;
        startHazardPolling();
    }

    // Refreshes text/styles of all layer toggle buttons.
    private void updateToggleLabels() {
        toggleUiController.updateLabels(
                mapController,
                poiToggleButton,
                hazardToggleButton,
                weatherToggleButton,
                showWeather
        );
    }

    // Recomputes reserve state and refreshes related UI sections.
    private void updateReserveState() {
        currentReserveState = reserveStateResolver.resolve(currentUserLatLng, reserves, allHazards);
        boolean showHazards = mapController.isShowingHazards();

        if (currentReserveState.isNoLocation()) {
            renderNoLocationState(showHazards);
            return;
        }

        if (currentReserveState.hasActiveReserve()) {
            renderInsideReserveState(currentReserveState.getActiveReserve());
        } else {
            renderOutsideReserveState();
        }

        eventCountText.setText(showHazards
                ? hazardCountTextForState(currentReserveState)
                : getString(R.string.no_hazards_total));
        finalizeReserveStateRefresh();
    }

    // Renders UI state when user location is not available yet.
    private void renderNoLocationState(boolean showHazards) {
        locationHintText.setText(hasLocationPermission()
                ? R.string.location_unavailable
                : R.string.status_location_permission_needed);
        eventCountText.setText(showHazards ? hazardCountTextForState(currentReserveState) : getString(R.string.no_hazards_total));
        finalizeReserveStateRefresh();
    }

    // Renders UI state when user is inside a known reserve.
    private void renderInsideReserveState(Reserve reserve) {
        locationHintText.setText(getString(R.string.inside_reserve, reserve.getDisplayName()));
        selectReserveInSpinner(reserve.getId());
    }

    // Renders UI state when user is outside known reserves.
    private void renderOutsideReserveState() {
        locationHintText.setText(R.string.outside_reserve);
    }

    // Runs shared UI refresh steps after reserve state updates.
    private void finalizeReserveStateRefresh() {
        updateReportLocationText();
        refreshMapContent();
        refreshWeather(false);
    }

    // Builds hazard-count message for the currently resolved reserve state.
    private String hazardCountTextForState(ReserveState reserveState) {
        int count = reserveState.getVisibleHazardCount();
        if (!reserveState.hasActiveReserve()) {
            return count == 0 ? getString(R.string.no_hazards_total) : getString(R.string.hazard_count_total, count);
        }
        return count == 0 ? getString(R.string.no_hazards_inside) : getString(R.string.hazard_count_inside, count);
    }

    // Selects the matching reserve item in spinner by id.
    private void selectReserveInSpinner(long reserveId) {
        for (int index = 0; index < reserves.size(); index++) {
            if (reserves.get(index).getId() == reserveId && reserveSpinner.getSelectedItemPosition() != index) {
                reserveSpinner.setSelection(index);
                return;
            }
        }
    }

    // Starts continuous location updates through the dedicated controller.
    private void startLocationTracking() {
        if (locationController != null) {
            locationController.startTracking(googleMap, locationControllerHost);
        }
    }

    // Applies a resolved location and refreshes reserve state, with optional recentering.
    private void applyCurrentLocation(LatLng latLng, boolean centerCamera, boolean animatedCamera) {
        currentUserLatLng = latLng;
        if (centerCamera) {
            moveCameraToUser(animatedCamera);
        }
        updateReserveState();
    }

    // Moves/animates camera to current user position.
    private void moveCameraToUser(boolean animated) {
        if (googleMap == null || currentUserLatLng == null) {
            showShortToast(R.string.location_unavailable);
            return;
        }
        hasCenteredOnUser = true;
        CameraPosition position = new CameraPosition.Builder()
                .target(currentUserLatLng)
                .zoom(Math.max(googleMap.getCameraPosition().zoom, 14f))
                .bearing(googleMap.getCameraPosition().bearing)
                .tilt(googleMap.getCameraPosition().tilt)
                .build();
        if (animated) {
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
        } else {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
    }

    // Resets map camera bearing/tilt back to north-up flat view.
    private void resetMapOrientation() {
        if (googleMap == null) {
            return;
        }
        CameraPosition current = googleMap.getCameraPosition();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder(current).bearing(0f).tilt(0f).build()
        ));
    }

    // Creates camera output Uri and launches picture capture.
    private void launchCameraCapture() {
        try {
            takePictureLauncher.launch(reportMediaController.prepareCameraCapture());
        } catch (Exception exception) {
            showShortToast(R.string.camera_capture_failed);
        }
    }

    // Validates form, captures fresh location, and uploads traveler report.
    private void submitTravelerReport() {
        Object selectedType = reportTypeSpinner.getSelectedItem();
        reportSubmissionController.submitReport(
                getSelectedReserve(),
                selectedType == null ? null : selectedType.toString(),
                reporterNameInput.getText().toString(),
                reportDescriptionInput.getText().toString(),
                reportUiHelper.getManualReportLatLng(),
                reportMediaController.getSelectedMediaUris(),
                hasLocationPermission(),
                reportSubmissionHost
        );
    }

    // Resets form fields after successful report submission.
    private void clearReportForm() {
        reporterNameInput.setText("");
        reportDescriptionInput.setText("");
        reportMediaController.clearSelectedMedia();
    }

    // Clears manual report pin/location so next report starts with a clean state.
    private void clearManualReportLocation() {
        reportUiHelper.clearManualLocation(manualLocationButton, reportLocationText, currentUserLatLng, this);
    }

    // Disables/enables interactive controls while background work runs.
    private void setBusyState(boolean busy, String message) {
        setStatusText(message);
        boolean enabled = !busy;
        setEnabled(attachMediaButton, enabled);
        setEnabled(capturePhotoButton, enabled);
        setEnabled(submitReportButton, enabled);
        setEnabled(manualLocationButton, enabled);
        setEnabled(reserveSpinner, enabled);
        setEnabled(reportTypeSpinner, enabled);
        setEnabled(reportToggleButton, enabled);
        setEnabled(poiToggleButton, enabled);
        setEnabled(hazardToggleButton, enabled);
        setEnabled(weatherToggleButton, enabled);
        setEnabled(menuButton, enabled);
        setEnabled(myLocationButton, enabled);
        setEnabled(northUpButton, enabled);
    }

    // Small helper to set enabled state for any view.
    private void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
    }

    // Updates the main status line text in header card.
    private void setStatusText(String message) {
        statusText.setText(message);
    }

    // Updates drawer server status text and color.
    private void updateServerStatus(Boolean online) {
        if (online == null) {
            serverStatusText.setText(R.string.server_status_checking);
            serverStatusText.setTextColor(COLOR_STATUS_CHECKING);
            return;
        }

        if (online) {
            serverStatusText.setText(R.string.server_status_online);
            serverStatusText.setTextColor(COLOR_STATUS_ONLINE);
        } else {
            serverStatusText.setText(R.string.server_status_offline);
            serverStatusText.setTextColor(COLOR_STATUS_OFFLINE);
        }
    }

    // Shows a short toast message from string resources.
    private void showShortToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    // Delegates weather UI/data refresh to weather controller.
    private void refreshWeather(boolean forceRefresh) {
        weatherController.refreshWeather(
                showWeather,
                forceRefresh,
                currentUserLatLng,
                weatherOverlay,
                weatherNowText,
                weatherHourlyPanel,
                weatherHourlyText,
                weatherExpandButton
        );
    }

    // Updates report panel with latest location coordinates.
    private void updateReportLocationText() {
        reportUiHelper.updateReportLocationText(reportLocationText, currentUserLatLng, this);
    }

    // Returns currently selected reserve from spinner.
    private Reserve getSelectedReserve() {
        int position = reserveSpinner.getSelectedItemPosition();
        return position >= 0 && position < reserves.size() ? reserves.get(position) : null;
    }

    // Checks whether either fine or coarse location permission is granted.
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Cleans up listeners and background executor when activity is destroyed.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopHazardPolling();
        if (locationController != null) {
            locationController.stopTracking();
        }
        executorService.shutdownNow();
    }
}
