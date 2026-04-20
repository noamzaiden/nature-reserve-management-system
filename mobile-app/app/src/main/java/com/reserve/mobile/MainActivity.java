package com.reserve.mobile;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
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
    private static final int COLOR_MENU_DEFAULT = android.graphics.Color.parseColor("#466B51");
    private static final int COLOR_MENU_ALERT = android.graphics.Color.parseColor("#B4473A");
    private static final String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    // UI fields
    private Spinner reserveSpinner;
    private Spinner reportTypeSpinner;
    private TextView locationHintText;
    private TextView reserveAlertText;
    private TextView statusText;
    private TextView hazardCountText;
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
    private MaterialButton reportToggleButton;
    private MaterialButton poiToggleButton;
    private MaterialButton hazardToggleButton;
    private TextView northUpButton;
    private ImageButton mapLayersButton;
    private ImageButton myLocationButton;
    private ImageButton weatherToggleButton;
    private ImageButton weatherExpandButton;
    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
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
    private final List<Poi> allPois = new ArrayList<>();
    private final List<Event> allHazards = new ArrayList<>();
    private final ReserveApiClient reserveApiClient = new ReserveApiClient();
    private final ReportApiClient reportApiClient = new ReportApiClient();
    private final WeatherApiClient weatherApiClient = new WeatherApiClient();
    private final ReserveStateResolver reserveStateResolver = new ReserveStateResolver();
    private final MapToggleUiController toggleUiController = new MapToggleUiController();
    private GoogleMap googleMap;
    private MapController mapController;
    private WeatherUiController weatherController;
    private EventPollingController hazardPollingController;
    private EventReportUiController reportUiController;
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
            resetReportDraft();
            reportUiController.setReportPanelVisible(false, reportPanel, bottomSpacer, reportToggleButton);
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
            statusText.setText(R.string.status_location_permission_needed);
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
            statusText.setText(R.string.status_location_waiting);
            updateReportLocationText();
            refreshWeather(false);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapController = new MapController(this);
        weatherController = new WeatherUiController(this, weatherApiClient, executorService);
        hazardPollingController = new EventPollingController(
                HAZARD_POLL_INTERVAL_MS,
                this::loadPublishedHazards
        );
        reportUiController = new EventReportUiController();
        locationController = new LocationController(fusedLocationClient);
        reportSubmissionController = new ReportSubmissionController(
                this,
                getContentResolver(),
                fusedLocationClient,
                executorService,
                reportApiClient
        );

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
        statusText.setText(R.string.status_loading_reserves);
        loadReserves();
    }

    private void bindViews() {
        reserveSpinner = findViewById(R.id.reserve_spinner);
        reportTypeSpinner = findViewById(R.id.report_type_spinner);
        locationHintText = findViewById(R.id.location_hint_text);
        reserveAlertText = findViewById(R.id.reserve_alert_text);
        statusText = findViewById(R.id.status_text);
        hazardCountText = findViewById(R.id.event_count_text);
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
        reportToggleButton = findViewById(R.id.report_toggle_button);
        poiToggleButton = findViewById(R.id.poi_toggle_button);
        hazardToggleButton = findViewById(R.id.hazard_toggle_button);
        northUpButton = findViewById(R.id.north_up_button);
        mapLayersButton = findViewById(R.id.map_layers_button);
        myLocationButton = findViewById(R.id.my_location_button);
        weatherToggleButton = findViewById(R.id.weather_toggle_button);
        weatherExpandButton = findViewById(R.id.weather_expand_button);
        drawerLayout = findViewById(R.id.drawer_layout);
        menuButton = findViewById(R.id.menu_button);
        reportPanel = findViewById(R.id.report_panel);
        bottomSpacer = findViewById(R.id.bottom_spacer);
        weatherOverlay = findViewById(R.id.weather_overlay);
        weatherHourlyPanel = findViewById(R.id.weather_hourly_panel);
    }

    private void configureTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.report_types)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(adapter);
    }

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
        attachMediaButton.setOnClickListener(view -> reportMediaController.openMediaPicker(mediaPickerLauncher));
        capturePhotoButton.setOnClickListener(view -> launchCameraCapture());
        submitReportButton.setOnClickListener(view -> submitTravelerReport());
        manualLocationButton.setOnClickListener(view -> startManualLocationSelection());
        reportToggleButton.setOnClickListener(view -> toggleReportPanel());
        menuButton.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));
        myLocationButton.setOnClickListener(view -> {
            followUserCamera = true;
            moveCameraToUser(true);
        });
        northUpButton.setOnClickListener(view -> resetMapOrientation());
        mapLayersButton.setOnClickListener(view -> showMapLayersMenu());
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

    private void configureMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setMapToolbarEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                followUserCamera = false;
            }
        });
        googleMap.setOnCameraIdleListener(() -> {
            if (mapController.shouldRefreshAfterCameraIdle()) {
                refreshMapContent();
            }
        });
        googleMap.setOnMapClickListener(this::handleMapClickForManualLocation);
        mapController.attachMap(map);
        refreshMapContent();
        startLocationTracking();
        statusText.setText("");
    }

    private void loadReserves() {
        executorService.execute(() -> {
            try {
                List<Reserve> loadedReserves = reserveApiClient.loadReserves();
                runOnUiThread(() -> onReservesLoaded(loadedReserves));
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    updateServerStatus(false);
                    statusText.setText(R.string.status_reserve_load_failed);
                });
            }
        });
    }

    private void loadPublishedHazards() {
        if (reserves.isEmpty() || hazardRefreshInFlight) {
            return;
        }
        hazardRefreshInFlight = true;
        executorService.execute(() -> {
            try {
                List<Event> loadedHazards = reserveApiClient.loadPublishedHazards(reserves);
                runOnUiThread(() -> onHazardsLoaded(loadedHazards));
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    updateServerStatus(false);
                    statusText.setText(R.string.status_hazard_load_failed);
                    hazardRefreshInFlight = false;
                });
            }
        });
    }

    private void loadReservePois() {
        if (reserves.isEmpty()) {
            return;
        }
        executorService.execute(() -> {
            try {
                List<Poi> loadedPois = reserveApiClient.loadPois(reserves);
                runOnUiThread(() -> onPoisLoaded(loadedPois));
            } catch (Exception exception) {
                runOnUiThread(() -> showShortToast(R.string.status_poi_load_failed));
            }
        });
    }

    private void toggleReportPanel() {
        boolean reportPanelVisible = reportUiController.toggleReportPanel(reportPanel, bottomSpacer, reportToggleButton);
        if (reportPanelVisible && !currentReserveState.hasActiveReserve()) {
            showShortToast(R.string.report_pick_location);
        }
    }

    private void startManualLocationSelection() {
        reportUiController.startManualLocationSelection(manualLocationButton);
        showShortToast(R.string.report_manual_location_hint);
    }

    private void handleMapClickForManualLocation(LatLng latLng) {
        if (!reportUiController.isSelectingManualLocation()) {
            return;
        }
        reportUiController.saveManualLocation(
                latLng,
                googleMap,
                manualLocationButton,
                reportLocationText,
                currentUserLatLng,
                this
        );
        showShortToast(R.string.report_manual_location_saved);
    }

    private void toggleWeather() {
        showWeather = !showWeather;
        updateToggleLabels();
        refreshWeather(showWeather);
    }

    private void refreshMapContent() {
        mapController.refresh(reserves, allPois, allHazards, currentReserveState.getActiveReserve(), hasLocationPermission());
        reportUiController.restoreManualLocationMarker(googleMap, this);
    }

    private void onMapLayerChanged(boolean updateReserveState) {
        refreshMapContent();
        updateToggleLabels();
        if (updateReserveState) {
            updateReserveState();
        }
    }

    private void onReservesLoaded(List<Reserve> loadedReserves) {
        updateServerStatus(true);
        reserves.clear();
        reserves.addAll(loadedReserves);
        ArrayAdapter<Reserve> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reserves);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reserveSpinner.setAdapter(adapter);
        refreshMapContent();
        updateReserveState();
        statusText.setText(R.string.status_loading_hazards);
        loadPublishedHazards();
        loadReservePois();
    }

    private void onPoisLoaded(List<Poi> loadedPois) {
        updateServerStatus(true);
        allPois.clear();
        allPois.addAll(loadedPois);
        refreshMapContent();
    }

    private void onHazardsLoaded(List<Event> loadedHazards) {
        updateServerStatus(true);
        allHazards.clear();
        allHazards.addAll(loadedHazards);
        refreshMapContent();
        updateReserveState();
        statusText.setText("");
        hazardRefreshInFlight = false;
        hazardPollingController.start();
    }

    private void updateToggleLabels() {
        toggleUiController.updateLabels(
                mapController,
                poiToggleButton,
                hazardToggleButton,
                weatherToggleButton,
                showWeather
        );
    }

    private void updateReserveState() {
        currentReserveState = reserveStateResolver.resolve(currentUserLatLng, reserves, allHazards);
        boolean showHazards = mapController.isShowingHazards();
        updateCurrentReserveAlert();

        if (currentReserveState.isNoLocation()) {
            renderNoLocationState(showHazards);
            return;
        }

        if (currentReserveState.hasActiveReserve()) {
            renderInsideReserveState(currentReserveState.getActiveReserve());
        } else {
            renderOutsideReserveState();
        }

        hazardCountText.setText(showHazards
                ? hazardCountTextForState(currentReserveState)
                : getString(R.string.no_hazards_total));
        finalizeReserveStateRefresh();
    }

    private void renderNoLocationState(boolean showHazards) {
        locationHintText.setText(hasLocationPermission()
                ? R.string.location_unavailable
                : R.string.status_location_permission_needed);
        hazardCountText.setText(showHazards
                ? hazardCountTextForState(currentReserveState)
                : getString(R.string.no_hazards_total));
        finalizeReserveStateRefresh();
    }

    private void renderInsideReserveState(Reserve reserve) {
        locationHintText.setText(getString(R.string.inside_reserve, reserve.getDisplayName()));
        selectReserveInSpinner(reserve.getId());
    }

    private void renderOutsideReserveState() {
        locationHintText.setText(R.string.outside_reserve);
    }

    private void finalizeReserveStateRefresh() {
        updateReportLocationText();
        refreshMapContent();
        refreshWeather(false);
    }

    private void updateCurrentReserveAlert() {
        if (currentReserveState.hasActiveReserveFireHazard()) {
            reserveAlertText.setVisibility(View.VISIBLE);
            menuButton.setBackgroundResource(R.drawable.bg_map_round_button_alert);
            menuButton.setColorFilter(COLOR_MENU_ALERT);
            menuButton.setContentDescription(getString(R.string.open_menu_fire_alert));
            return;
        }

        reserveAlertText.setVisibility(View.GONE);
        menuButton.setBackgroundResource(R.drawable.bg_map_round_button);
        menuButton.setColorFilter(COLOR_MENU_DEFAULT);
        menuButton.setContentDescription(getString(R.string.open_menu));
    }

    private String hazardCountTextForState(ReserveState reserveState) {
        int count = reserveState.getVisibleHazardCount();
        if (!reserveState.hasActiveReserve()) {
            return count == 0
                    ? getString(R.string.no_hazards_total)
                    : getResources().getQuantityString(R.plurals.hazard_count_total, count, count);
        }
        return count == 0
                ? getString(R.string.no_hazards_inside)
                : getResources().getQuantityString(R.plurals.hazard_count_inside, count, count);
    }

    private void selectReserveInSpinner(long reserveId) {
        for (int index = 0; index < reserves.size(); index++) {
            if (reserves.get(index).getId() == reserveId && reserveSpinner.getSelectedItemPosition() != index) {
                reserveSpinner.setSelection(index);
                return;
            }
        }
    }

    private void startLocationTracking() {
        if (locationController != null) {
            locationController.startTracking(googleMap, locationControllerHost);
        }
    }

    private void applyCurrentLocation(LatLng latLng, boolean centerCamera, boolean animatedCamera) {
        currentUserLatLng = latLng;
        if (centerCamera) {
            moveCameraToUser(animatedCamera);
        }
        updateReserveState();
    }

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

    private void resetMapOrientation() {
        if (googleMap == null) {
            return;
        }

        CameraPosition currentPosition = googleMap.getCameraPosition();
        CameraPosition northUpPosition = new CameraPosition.Builder(currentPosition)
                .bearing(0f)
                .tilt(0f)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(northUpPosition));
    }

    private void showMapLayersMenu() {
        if (googleMap == null) {
            return;
        }

        PopupMenu popupMenu = new PopupMenu(this, mapLayersButton);
        popupMenu.inflate(R.menu.map_layers_menu);

        int mapType = googleMap.getMapType();
        popupMenu.getMenu().findItem(R.id.layer_type_normal).setChecked(mapType == GoogleMap.MAP_TYPE_NORMAL);
        popupMenu.getMenu().findItem(R.id.layer_type_satellite).setChecked(mapType == GoogleMap.MAP_TYPE_SATELLITE);
        popupMenu.getMenu().findItem(R.id.layer_type_terrain).setChecked(mapType == GoogleMap.MAP_TYPE_TERRAIN);
        popupMenu.getMenu().findItem(R.id.layer_type_hybrid).setChecked(mapType == GoogleMap.MAP_TYPE_HYBRID);
        popupMenu.getMenu().findItem(R.id.layer_traffic).setChecked(googleMap.isTrafficEnabled());

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.layer_type_normal) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            }
            if (itemId == R.id.layer_type_satellite) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;
            }
            if (itemId == R.id.layer_type_terrain) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                return true;
            }
            if (itemId == R.id.layer_type_hybrid) {
                googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                return true;
            }
            if (itemId == R.id.layer_traffic) {
                googleMap.setTrafficEnabled(!googleMap.isTrafficEnabled());
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void launchCameraCapture() {
        try {
            takePictureLauncher.launch(reportMediaController.prepareCameraCapture());
        } catch (Exception exception) {
            showShortToast(R.string.camera_capture_failed);
        }
    }

    private void submitTravelerReport() {
        Object selectedType = reportTypeSpinner.getSelectedItem();
        reportSubmissionController.submitReport(
                getSelectedReserve(),
                selectedType == null ? null : selectedType.toString(),
                reporterNameInput.getText().toString(),
                reportDescriptionInput.getText().toString(),
                reportUiController.getManualReportLatLng(),
                reportMediaController.getSelectedMediaUris(),
                hasLocationPermission(),
                reportSubmissionHost
        );
    }

    private void resetReportDraft() {
        reporterNameInput.setText("");
        reportDescriptionInput.setText("");
        reportMediaController.clearSelectedMedia();
        reportUiController.clearManualLocation(manualLocationButton, reportLocationText, currentUserLatLng, this);
    }

    private void setBusyState(boolean busy, String message) {
        statusText.setText(message);
        boolean enabled = !busy;
        View[] controls = {
                attachMediaButton,
                capturePhotoButton,
                submitReportButton,
                manualLocationButton,
                reserveSpinner,
                reportTypeSpinner,
                reportToggleButton,
                poiToggleButton,
                hazardToggleButton,
                weatherToggleButton,
                northUpButton,
                mapLayersButton,
                menuButton,
                myLocationButton,
                weatherExpandButton
        };
        for (View control : controls) {
            control.setEnabled(enabled);
        }
    }

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

    private void showShortToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

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

    private void updateReportLocationText() {
        reportUiController.updateReportLocationText(reportLocationText, currentUserLatLng, this);
    }

    private Reserve getSelectedReserve() {
        int position = reserveSpinner.getSelectedItemPosition();
        return position >= 0 && position < reserves.size() ? reserves.get(position) : null;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hazardPollingController.stop();
        if (locationController != null) {
            locationController.stopTracking();
        }
        executorService.shutdownNow();
    }
}
