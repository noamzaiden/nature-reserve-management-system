package com.reserve.mobile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<ReserveOption> reserves = new ArrayList<>();
    private final List<PublicEvent> allHazards = new ArrayList<>();
    private final List<Uri> selectedMediaUris = new ArrayList<>();
    private final ReserveRepository reserveRepository = new ReserveRepository();
    private final WeatherRepository weatherRepository = new WeatherRepository();

    private Spinner reserveSpinner;
    private Spinner reportTypeSpinner;
    private TextView reserveNameText;
    private TextView locationHintText;
    private TextView statusText;
    private TextView eventCountText;
    private TextView weatherText;
    private TextView selectedMediaText;
    private TextView reportLocationText;
    private TextView serverStatusText;
    private EditText reporterNameInput;
    private EditText reportDescriptionInput;
    private Button attachMediaButton;
    private Button capturePhotoButton;
    private Button submitReportButton;
    private TextView northUpButton;
    private MaterialButton reportToggleButton;
    private MaterialButton poiToggleButton;
    private MaterialButton hazardToggleButton;
    private MaterialButton weatherToggleButton;
    private DrawerLayout drawerLayout;
    private ImageButton menuButton;
    private ImageButton myLocationButton;
    private View reportPanel;

    private ActivityResultLauncher<String[]> mediaPickerLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private GoogleMap googleMap;
    private ReserveMapHelper reserveMapHelper;
    private LatLng currentUserLatLng;
    private LatLng lastWeatherLatLng;
    private ReserveOption currentReserve;
    private WeatherInfo currentWeather;
    private boolean reportPanelVisible = false;
    private boolean followUserCamera = true;
    private boolean hasCenteredOnUser = false;
    private boolean showWeather = false;
    private Uri pendingCameraPhotoUri;
    private long lastWeatherLoadedAt = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        reserveMapHelper = new ReserveMapHelper(this);

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

    private void bindViews() {
        reserveSpinner = findViewById(R.id.reserve_spinner);
        reportTypeSpinner = findViewById(R.id.report_type_spinner);
        reserveNameText = findViewById(R.id.reserve_name_text);
        locationHintText = findViewById(R.id.location_hint_text);
        statusText = findViewById(R.id.status_text);
        eventCountText = findViewById(R.id.event_count_text);
        weatherText = findViewById(R.id.weather_text);
        selectedMediaText = findViewById(R.id.selected_media_text);
        reportLocationText = findViewById(R.id.report_location_text);
        serverStatusText = findViewById(R.id.server_status_text);
        reporterNameInput = findViewById(R.id.reporter_name_input);
        reportDescriptionInput = findViewById(R.id.report_description_input);
        attachMediaButton = findViewById(R.id.attach_media_button);
        capturePhotoButton = findViewById(R.id.capture_photo_button);
        submitReportButton = findViewById(R.id.submit_report_button);
        northUpButton = findViewById(R.id.north_up_button);
        reportToggleButton = findViewById(R.id.report_toggle_button);
        poiToggleButton = findViewById(R.id.poi_toggle_button);
        hazardToggleButton = findViewById(R.id.hazard_toggle_button);
        weatherToggleButton = findViewById(R.id.weather_toggle_button);
        drawerLayout = findViewById(R.id.drawer_layout);
        menuButton = findViewById(R.id.menu_button);
        myLocationButton = findViewById(R.id.my_location_button);
        reportPanel = findViewById(R.id.report_panel);
    }

    private void configureTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"FIRE", "BLOCKAGE", "OTHER"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(adapter);
    }

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
                        Toast.makeText(this, R.string.camera_attachment_added, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.camera_capture_cancelled, Toast.LENGTH_SHORT).show();
                    }
                    pendingCameraPhotoUri = null;
                    updateSelectedMediaText();
                }
        );
    }

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

    private void configureButtons() {
        attachMediaButton.setOnClickListener(view -> mediaPickerLauncher.launch(new String[]{"image/*", "video/*"}));
        capturePhotoButton.setOnClickListener(view -> launchCameraCapture());
        submitReportButton.setOnClickListener(view -> submitTravelerReport());
        reportToggleButton.setOnClickListener(view -> toggleReportPanel());
        menuButton.setOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));
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
        weatherToggleButton.setOnClickListener(view -> {
            showWeather = !showWeather;
            updateToggleLabels();
            refreshWeather(showWeather);
        });
        myLocationButton.setOnClickListener(view -> {
            followUserCamera = true;
            moveCameraToUser(true);
        });
        northUpButton.setOnClickListener(view -> resetMapOrientation());
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
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                followUserCamera = false;
            }
        });
        reserveMapHelper.attachMap(map);
        refreshMapContent();
        startLocationTracking();
        setStatusText(getString(R.string.status_map_ready));
    }

    private void loadReserves() {
        executorService.execute(() -> {
            try {
                List<ReserveOption> loadedReserves = reserveRepository.loadReserves();
                runOnUiThread(() -> {
                    updateServerStatus(true);
                    reserves.clear();
                    reserves.addAll(loadedReserves);
                    ArrayAdapter<ReserveOption> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reserves);
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

    private void loadPublishedHazards() {
        executorService.execute(() -> {
            try {
                List<PublicEvent> loadedHazards = reserveRepository.loadPublishedHazards(reserves);
                runOnUiThread(() -> {
                    updateServerStatus(true);
                    allHazards.clear();
                    allHazards.addAll(loadedHazards);
                    refreshMapContent();
                    updateReserveSummary();
                    setStatusText(getString(R.string.status_map_ready));
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    updateServerStatus(false);
                    setStatusText(getString(R.string.status_hazard_load_failed));
                });
            }
        });
    }

    private void toggleReportPanel() {
        reportPanelVisible = !reportPanelVisible;
        reportPanel.setVisibility(reportPanelVisible ? android.view.View.VISIBLE : android.view.View.GONE);
        reportToggleButton.setText(reportPanelVisible ? R.string.hide_report_button : R.string.report_event_button);
        if (reportPanelVisible && currentReserve == null) {
            Toast.makeText(this, R.string.report_pick_location, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshMapContent() {
        reserveMapHelper.refresh(reserves, allHazards, currentReserve, hasLocationPermission());
    }

    private void updateToggleLabels() {
        boolean showPois = reserveMapHelper.isShowingPois();
        boolean showHazards = reserveMapHelper.isShowingHazards();

        poiToggleButton.setText(showPois ? R.string.poi_on : R.string.poi_off);
        hazardToggleButton.setText(showHazards ? R.string.hazards_on : R.string.hazards_off);
        weatherToggleButton.setText(showWeather ? R.string.display_weather_on : R.string.display_weather_off);

        int activeFill = Color.parseColor("#E3F0E2");
        int inactiveFill = Color.parseColor("#F8FCF7");
        int activeStroke = Color.parseColor("#5E8B67");
        int inactiveStroke = Color.parseColor("#A4BCA8");

        poiToggleButton.setBackgroundTintList(ColorStateList.valueOf(showPois ? activeFill : inactiveFill));
        poiToggleButton.setStrokeColor(ColorStateList.valueOf(showPois ? activeStroke : inactiveStroke));
        hazardToggleButton.setBackgroundTintList(ColorStateList.valueOf(showHazards ? activeFill : inactiveFill));
        hazardToggleButton.setStrokeColor(ColorStateList.valueOf(showHazards ? activeStroke : inactiveStroke));
        weatherToggleButton.setBackgroundTintList(ColorStateList.valueOf(showWeather ? activeFill : inactiveFill));
        weatherToggleButton.setStrokeColor(ColorStateList.valueOf(showWeather ? activeStroke : inactiveStroke));
    }

    private void updateReserveSummary() {
        currentReserve = currentUserLatLng == null ? null : ReserveUtils.findReserveForLocation(reserves, currentUserLatLng);
        boolean showHazards = reserveMapHelper.isShowingHazards();

        if (currentUserLatLng == null) {
            reserveNameText.setText(R.string.map_heading);
            locationHintText.setText(hasLocationPermission() ? R.string.location_unavailable : R.string.status_location_permission_needed);
            eventCountText.setText(showHazards ? hazardCountTextForReserve(null) : getString(R.string.no_hazards_total));
            updateReportLocationText();
            refreshMapContent();
            refreshWeather(false);
            return;
        }

        if (currentReserve != null) {
            reserveNameText.setText(currentReserve.getDisplayName());
            locationHintText.setText(getString(R.string.inside_reserve, currentReserve.getDisplayName()));
            selectReserveInSpinner(currentReserve.getId());
        } else {
            reserveNameText.setText(R.string.map_heading);
            ReserveOption nearestReserve = ReserveUtils.findNearestReserve(reserves, currentUserLatLng);
            locationHintText.setText(nearestReserve == null
                    ? getString(R.string.outside_reserve)
                    : getString(R.string.nearest_reserve, nearestReserve.getDisplayName()));
        }

        eventCountText.setText(showHazards ? hazardCountTextForReserve(currentReserve) : getString(R.string.no_hazards_total));
        updateReportLocationText();
        refreshMapContent();
        refreshWeather(false);
    }

    private String hazardCountTextForReserve(ReserveOption reserve) {
        int count = ReserveUtils.countVisibleHazards(allHazards, reserve);
        if (reserve == null) {
            return count == 0 ? getString(R.string.no_hazards_total) : getString(R.string.hazard_count_total, count);
        }
        return count == 0 ? getString(R.string.no_hazards_inside) : getString(R.string.hazard_count_inside, count);
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
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }
        enableMyLocationLayer();
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
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
    private void enableMyLocationLayer() {
        if (googleMap != null && hasLocationPermission()) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void moveCameraToUser(boolean animated) {
        if (googleMap == null || currentUserLatLng == null) {
            Toast.makeText(this, R.string.location_unavailable, Toast.LENGTH_SHORT).show();
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
        CameraPosition current = googleMap.getCameraPosition();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder(current).bearing(0f).tilt(0f).build()
        ));
    }

    private void launchCameraCapture() {
        try {
            pendingCameraPhotoUri = createCameraImageUri();
            takePictureLauncher.launch(pendingCameraPhotoUri);
        } catch (Exception exception) {
            pendingCameraPhotoUri = null;
            Toast.makeText(this, R.string.camera_capture_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private Uri createCameraImageUri() throws Exception {
        File picturesDir = new File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "reports");
        if (!picturesDir.exists() && !picturesDir.mkdirs()) {
            throw new IllegalStateException("Could not create report picture directory");
        }
        File photoFile = new File(picturesDir, "report_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
    }

    private void submitTravelerReport() {
        ReserveOption selectedReserve = getSelectedReserve();
        if (selectedReserve == null) {
            Toast.makeText(this, R.string.report_requires_reserve, Toast.LENGTH_SHORT).show();
            return;
        }
        String description = reportDescriptionInput.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, R.string.report_requires_description, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasLocationPermission()) {
            Toast.makeText(this, R.string.report_requires_location, Toast.LENGTH_SHORT).show();
            startLocationTracking();
            return;
        }

        setBusyState(true, getString(R.string.status_fetching_phone_location));
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        setBusyState(false, getString(R.string.status_location_waiting));
                        Toast.makeText(this, R.string.report_requires_location, Toast.LENGTH_SHORT).show();
                        updateReportLocationText();
                        return;
                    }
                    currentUserLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    updateReserveSummary();
                    setBusyState(true, getString(R.string.status_report_sending));
                    executorService.execute(() -> {
                        try {
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
                                reporterNameInput.setText("");
                                reportDescriptionInput.setText("");
                                selectedMediaUris.clear();
                                updateSelectedMediaText();
                                Toast.makeText(this, R.string.report_sent_toast, Toast.LENGTH_LONG).show();
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
                })
                .addOnFailureListener(exception -> {
                    setBusyState(false, getString(R.string.status_location_waiting));
                    Toast.makeText(this, R.string.report_requires_location, Toast.LENGTH_SHORT).show();
                    updateReportLocationText();
                });
    }

    private void setBusyState(boolean busy, String message) {
        setStatusText(message);
        attachMediaButton.setEnabled(!busy);
        capturePhotoButton.setEnabled(!busy);
        submitReportButton.setEnabled(!busy);
        reserveSpinner.setEnabled(!busy);
        reportTypeSpinner.setEnabled(!busy);
        reportToggleButton.setEnabled(!busy);
        poiToggleButton.setEnabled(!busy);
        hazardToggleButton.setEnabled(!busy);
        weatherToggleButton.setEnabled(!busy);
        menuButton.setEnabled(!busy);
        myLocationButton.setEnabled(!busy);
        northUpButton.setEnabled(!busy);
    }

    private void setStatusText(String message) {
        statusText.setText(message);
    }

    private void updateServerStatus(Boolean online) {
        if (online == null) {
            serverStatusText.setText(R.string.server_status_checking);
            serverStatusText.setTextColor(Color.parseColor("#735A2E"));
            return;
        }

        if (online) {
            serverStatusText.setText(R.string.server_status_online);
            serverStatusText.setTextColor(Color.parseColor("#2C7A57"));
        } else {
            serverStatusText.setText(R.string.server_status_offline);
            serverStatusText.setTextColor(Color.parseColor("#B4473A"));
        }
    }

    private void refreshWeather(boolean forceRefresh) {
        if (!showWeather) {
            weatherText.setVisibility(View.GONE);
            return;
        }

        weatherText.setVisibility(View.VISIBLE);

        if (currentUserLatLng == null) {
            weatherText.setText(R.string.weather_waiting_location);
            return;
        }

        if (!weatherRepository.hasApiKey()) {
            weatherText.setText(R.string.weather_missing_key);
            return;
        }

        if (!forceRefresh && !shouldReloadWeather(currentUserLatLng)) {
            if (currentWeather != null) {
                weatherText.setText(getString(
                        R.string.weather_ready,
                        currentWeather.getTemperatureCelsius(),
                        currentWeather.getCondition()
                ));
            }
            return;
        }

        weatherText.setText(R.string.weather_loading);
        LatLng requestLocation = currentUserLatLng;
        executorService.execute(() -> {
            try {
                WeatherInfo loadedWeather = weatherRepository.loadCurrentWeather(
                        requestLocation.latitude,
                        requestLocation.longitude
                );
                runOnUiThread(() -> {
                    currentWeather = loadedWeather;
                    lastWeatherLatLng = requestLocation;
                    lastWeatherLoadedAt = System.currentTimeMillis();
                    if (showWeather) {
                        weatherText.setText(getString(
                                R.string.weather_ready,
                                loadedWeather.getTemperatureCelsius(),
                                loadedWeather.getCondition()
                        ));
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (showWeather) {
                        weatherText.setText(R.string.weather_unavailable);
                    }
                });
            }
        });
    }

    private boolean shouldReloadWeather(LatLng nowLocation) {
        if (currentWeather == null || lastWeatherLatLng == null) {
            return true;
        }

        long age = System.currentTimeMillis() - lastWeatherLoadedAt;
        if (age > 10 * 60 * 1000) {
            return true;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                lastWeatherLatLng.latitude,
                lastWeatherLatLng.longitude,
                nowLocation.latitude,
                nowLocation.longitude,
                results
        );
        return results[0] > 1000;
    }

    private void updateReportLocationText() {
        if (currentUserLatLng == null) {
            reportLocationText.setText(R.string.report_location_waiting);
            return;
        }
        reportLocationText.setText(getString(
                R.string.report_location_ready,
                currentUserLatLng.latitude,
                currentUserLatLng.longitude
        ));
    }

    private ReserveOption getSelectedReserve() {
        int position = reserveSpinner.getSelectedItemPosition();
        return position >= 0 && position < reserves.size() ? reserves.get(position) : null;
    }

    private void updateSelectedMediaText() {
        selectedMediaText.setText(selectedMediaUris.isEmpty()
                ? getString(R.string.selected_media_none)
                : selectedMediaUris.size() + " attachment(s) selected");
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        executorService.shutdownNow();
    }
}
