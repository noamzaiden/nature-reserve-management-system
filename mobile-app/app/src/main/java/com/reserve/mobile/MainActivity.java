package com.reserve.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
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
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final long HAZARD_POLL_INTERVAL_MS = 15000L;
    private static final String[] REPORT_MEDIA_MIME_TYPES = {"image/*", "video/*"};
    private static final int COLOR_STATUS_CHECKING = android.graphics.Color.parseColor("#735A2E");
    private static final int COLOR_STATUS_ONLINE = android.graphics.Color.parseColor("#2C7A57");
    private static final int COLOR_STATUS_OFFLINE = android.graphics.Color.parseColor("#B4473A");

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<Reserve> reserves = new ArrayList<>();
    private final List<PublicEvent> allHazards = new ArrayList<>();
    private final List<Uri> selectedMediaUris = new ArrayList<>();
    private final ReserveRepository reserveRepository = new ReserveRepository();
    private final WeatherRepository weatherRepository = new WeatherRepository();
    private final MainToggleUiController toggleUiController = new MainToggleUiController();

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

    private ActivityResultLauncher<String[]> mediaPickerLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private GoogleMap googleMap;
    private ReserveMapHelper reserveMapHelper;
    private MainWeatherController weatherController;
    private MainHazardPollingController hazardPollingController;
    private MainReportUiHelper reportUiHelper;
    private LatLng currentUserLatLng;
    private Reserve currentReserve;
    private boolean followUserCamera = true;
    private boolean hasCenteredOnUser = false;
    private boolean showWeather = false;
    private boolean hazardRefreshInFlight = false;
    private Uri pendingCameraPhotoUri;

    @Override
    // Initializes screen state, listeners, map, and first backend load.
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Core helpers used by map/location and network tasks.
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        reserveMapHelper = new ReserveMapHelper(this);
        weatherController = new MainWeatherController(this, weatherRepository, executorService);
        hazardPollingController = new MainHazardPollingController(
                HAZARD_POLL_INTERVAL_MS,
                () -> !reserves.isEmpty(),
                this::loadPublishedHazards
        );
        reportUiHelper = new MainReportUiHelper();

        // Small setup pipeline so startup order is easy to follow.
        bindViews();
        configureTypeSpinner();
        configureMediaPicker();
        configureLocationPermissionLauncher();
        configureButtons();
        configureMap();
        updateToggleLabels();
        updateServerStatus(null);
        updateSelectedMediaText();
        updateReportLocationText();
        setStatusText(getString(R.string.status_loading_reserves));
        loadReserves();
    }

    // Finds and caches all views from the activity layout.
    private void bindViews() {
        // Keep all findViewById calls in one place.
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
                new String[]{"FIRE", "BLOCKAGE", "OTHER"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(adapter);
    }

    // Registers media picker and camera activity result callbacks.
    private void configureMediaPicker() {
        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    if (uris != null) {
                        selectedMediaUris.addAll(uris);
                    }
                    updateSelectedMediaText();
                }
        );
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success) && pendingCameraPhotoUri != null) {
                        selectedMediaUris.add(pendingCameraPhotoUri);
                        showShortToast(R.string.camera_attachment_added);
                    } else {
                        showShortToast(R.string.camera_capture_cancelled);
                    }
                    pendingCameraPhotoUri = null;
                    updateSelectedMediaText();
                }
        );
    }

    // Registers location permission request callback.
    private void configureLocationPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                            || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (granted) {
                        startLocationTracking();
                    } else {
                        setStatusText(getString(R.string.status_location_permission_needed));
                        updateReserveSummary();
                    }
                }
        );
    }

    // Wires all button click listeners in grouped setup methods.
    private void configureButtons() {
        // Group listeners by feature so this method stays short.
        configureReportButtons();
        configureDrawerAndMapButtons();
        configureLayerButtons();
    }

    // Connects report panel actions (attach, camera, submit, panel toggle).
    private void configureReportButtons() {
        attachMediaButton.setOnClickListener(view -> mediaPickerLauncher.launch(REPORT_MEDIA_MIME_TYPES));
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
            reserveMapHelper.setShowPois(!reserveMapHelper.isShowingPois());
            refreshMapContent();
            updateToggleLabels();
        });
        hazardToggleButton.setOnClickListener(view -> {
            reserveMapHelper.setShowHazards(!reserveMapHelper.isShowingHazards());
            refreshMapContent();
            updateToggleLabels();
            updateReserveSummary();
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

    @Override
    // Called when GoogleMap is ready; enables map features and tracking.
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
        reserveMapHelper.attachMap(map);
        refreshMapContent();
        startLocationTracking();
        setStatusText("");
    }

    // Loads reserve list from backend and updates UI state.
    private void loadReserves() {
        executorService.execute(() -> {
            try {
                List<Reserve> loadedReserves = reserveRepository.loadReserves();
                runOnUiThread(() -> {
                    // UI updates must run on the main thread.
                    updateServerStatus(true);
                    reserves.clear();
                    reserves.addAll(loadedReserves);
                    ArrayAdapter<Reserve> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reserves);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    reserveSpinner.setAdapter(adapter);
                    refreshMapContent();
                    updateReserveSummary();
                    setStatusText(getString(R.string.status_loading_hazards));
                    loadPublishedHazards();
                });
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
        if (hazardRefreshInFlight) {
            return;
        }
        hazardRefreshInFlight = true;
        executorService.execute(() -> {
            try {
                List<PublicEvent> loadedHazards = reserveRepository.loadPublishedHazards(reserves);
                runOnUiThread(() -> {
                    // Replace the full list so map refresh always uses latest server state.
                    updateServerStatus(true);
                    allHazards.clear();
                    allHazards.addAll(loadedHazards);
                    refreshMapContent();
                    updateReserveSummary();
                    setStatusText("");
                    hazardRefreshInFlight = false;
                    startHazardPolling();
                });
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
        if (reportPanelVisible && currentReserve == null) {
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
        reserveMapHelper.refresh(reserves, allHazards, currentReserve, hasLocationPermission());
    }

    // Refreshes text/styles of all layer toggle buttons.
    private void updateToggleLabels() {
        toggleUiController.updateLabels(
                reserveMapHelper,
                poiToggleButton,
                hazardToggleButton,
                weatherToggleButton,
                showWeather
        );
    }

    // Recomputes reserve status text and hazard counters for current location.
    private void updateReserveSummary() {
        // Figure out whether the user is currently inside a known reserve.
        currentReserve = currentUserLatLng == null ? null : ReserveUtils.findReserveForLocation(reserves, currentUserLatLng);
        boolean showHazards = reserveMapHelper.isShowingHazards();

        if (currentUserLatLng == null) {
            renderNoLocationSummary(showHazards);
            return;
        }

        if (currentReserve != null) {
            renderInsideReserveSummary(currentReserve);
        } else {
            renderOutsideReserveSummary();
        }

        eventCountText.setText(showHazards
                ? hazardCountTextForReserve(currentReserve)
                : getString(R.string.no_hazards_total));
        finalizeSummaryRefresh();
    }

    // Renders UI state when user location is not available yet.
    private void renderNoLocationSummary(boolean showHazards) {
        locationHintText.setText(hasLocationPermission()
                ? R.string.location_unavailable
                : R.string.status_location_permission_needed);
        eventCountText.setText(showHazards ? hazardCountTextForReserve(null) : getString(R.string.no_hazards_total));
        finalizeSummaryRefresh();
    }

    // Renders UI state when user is inside a known reserve.
    private void renderInsideReserveSummary(Reserve reserve) {
        locationHintText.setText(getString(R.string.inside_reserve, reserve.getDisplayName()));
        selectReserveInSpinner(reserve.getId());
    }

    // Renders UI state when user is outside known reserves.
    private void renderOutsideReserveSummary() {
        locationHintText.setText(R.string.outside_reserve);
    }

    // Runs shared UI refresh steps after summary text updates.
    private void finalizeSummaryRefresh() {
        updateReportLocationText();
        refreshMapContent();
        refreshWeather(false);
    }

    // Builds hazard-count message for one reserve or all reserves.
    private String hazardCountTextForReserve(Reserve reserve) {
        int count = ReserveUtils.countVisibleHazards(allHazards, reserve);
        if (reserve == null) {
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

    // Starts continuous location updates (requests permission if needed).
    private void startLocationTracking() {
        if (!hasLocationPermission()) {
            // Ask for either fine or coarse location, then retry from callback.
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        enableMyLocationLayer();
        if (locationCallback == null) {
            // Reuse one callback so we do not register multiple listeners.
            locationCallback = new LocationCallback() {
                @Override
                // Handles each new GPS update and refreshes reserve summary.
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location location = locationResult.getLastLocation();
                    if (location == null) {
                        return;
                    }
                    currentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (followUserCamera || !hasCenteredOnUser) {
                        moveCameraToUser(hasCenteredOnUser);
                    }
                    updateReserveSummary();
                }
            };
        }
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000)
                .build();
        // Try last known position first for faster first render.
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                setStatusText(getString(R.string.status_location_waiting));
                updateReportLocationText();
                refreshWeather(false);
            } else {
                currentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                moveCameraToUser(false);
                updateReserveSummary();
            }
        });
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    @SuppressLint("MissingPermission")
    // Enables map my-location layer when permission and map are ready.
    private void enableMyLocationLayer() {
        if (googleMap != null && hasLocationPermission()) {
            googleMap.setMyLocationEnabled(true);
        }
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
            pendingCameraPhotoUri = createCameraImageUri();
            takePictureLauncher.launch(pendingCameraPhotoUri);
        } catch (Exception exception) {
            pendingCameraPhotoUri = null;
            showShortToast(R.string.camera_capture_failed);
        }
    }

    // Creates a FileProvider Uri for a new report photo file.
    private Uri createCameraImageUri() throws Exception {
        File picturesDir = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "reports");
        if (!picturesDir.exists() && !picturesDir.mkdirs()) {
            throw new IllegalStateException("Could not create report picture directory");
        }
        File photoFile = new File(picturesDir, "report_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
    }

    // Validates form, captures fresh location, and uploads traveler report.
    private void submitTravelerReport() {
        Reserve selectedReserve = getSelectedReserve();
        if (selectedReserve == null) {
            showShortToast(R.string.report_requires_reserve);
            return;
        }
        String description = reportDescriptionInput.getText().toString().trim();
        if (description.isEmpty()) {
            showShortToast(R.string.report_requires_description);
            return;
        }
        LatLng manualLocation = reportUiHelper.getManualReportLatLng();
        if (manualLocation != null) {
            currentUserLatLng = manualLocation;
            updateReserveSummary();
            uploadTravelerReport(selectedReserve);
            return;
        }

        if (!hasLocationPermission()) {
            showShortToast(R.string.report_requires_location);
            startLocationTracking();
            return;
        }

        setBusyState(true, getString(R.string.status_fetching_phone_location));
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        // Grab a fresh point for the report so hazard coordinates are reliable.
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        handleMissingCurrentLocationForReport();
                        return;
                    }
                    currentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    updateReserveSummary();
                    uploadTravelerReport(selectedReserve);
                })
                .addOnFailureListener(exception -> handleMissingCurrentLocationForReport());
    }

    // Handles failure to get fresh GPS before sending a report.
    private void handleMissingCurrentLocationForReport() {
        setBusyState(false, getString(R.string.status_location_waiting));
        showShortToast(R.string.report_requires_location);
        updateReportLocationText();
    }

    // Uploads report using either GPS or manually picked coordinates.
    private void uploadTravelerReport(Reserve selectedReserve) {
        setBusyState(true, getString(R.string.status_report_sending));
        executorService.execute(() -> {
            try {
                // Build payload from current form values and selected media.
                TravelerReportData reportData = new TravelerReportData(
                        selectedReserve.getId(),
                        reportTypeSpinner.getSelectedItem().toString(),
                        reporterNameInput.getText().toString().trim(),
                        reportDescriptionInput.getText().toString().trim(),
                        currentUserLatLng.latitude,
                        currentUserLatLng.longitude,
                        selectedMediaUris
                );
                reserveRepository.submitTravelerReport(getContentResolver(), reportData);
                runOnUiThread(() -> {
                    updateServerStatus(true);
                    clearReportForm();
                    clearManualReportLocation();
                    reportUiHelper.setReportPanelVisible(false, reportPanel, bottomSpacer, reportToggleButton);
                    showLongToast(R.string.report_sent_toast);
                    setBusyState(false, getString(R.string.status_report_sent));
                    loadPublishedHazards();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    updateServerStatus(false);
                    setBusyState(false, getString(R.string.status_report_failed));
                });
            }
        });
    }

    // Resets form fields after successful report submission.
    private void clearReportForm() {
        reporterNameInput.setText("");
        reportDescriptionInput.setText("");
        selectedMediaUris.clear();
        updateSelectedMediaText();
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

    // Shows a long toast message from string resources.
    private void showLongToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show();
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

    // Shows currently selected attachment count in report panel.
    private void updateSelectedMediaText() {
        selectedMediaText.setText(selectedMediaUris.isEmpty()
                ? getString(R.string.selected_media_none)
                : selectedMediaUris.size() + " attachment(s) selected");
    }

    // Checks whether either fine or coarse location permission is granted.
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    // Cleans up listeners and background executor when activity is destroyed.
    protected void onDestroy() {
        super.onDestroy();
        stopHazardPolling();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        executorService.shutdownNow();
    }
}
