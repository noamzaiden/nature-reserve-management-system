package com.reserve.mobile;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

final class LocationController {

    interface Host {
        boolean hasLocationPermission();

        void requestLocationPermissions();

        void onLocationPermissionDenied();

        void onInitialLocationAvailable(LatLng latLng);

        void onLiveLocationAvailable(LatLng latLng);

        void onLocationUnavailable();
    }

    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean tracking;

    LocationController(FusedLocationProviderClient fusedLocationClient) {
        this.fusedLocationClient = fusedLocationClient;
    }

    // Continues or starts tracking after the permission result is known.
    void onPermissionResult(boolean granted, GoogleMap googleMap, Host host) {
        if (granted) {
            startTracking(googleMap, host);
        } else {
            host.onLocationPermissionDenied();
        }
    }

    @SuppressLint("MissingPermission")
    // Starts location tracking and reports initial/live updates back to the host.
    void startTracking(GoogleMap googleMap, Host host) {
        if (!host.hasLocationPermission()) {
            host.requestLocationPermissions();
            return;
        }

        enableMyLocationLayer(googleMap);
        ensureLocationCallback(host);

        if (tracking) {
            return;
        }

        tracking = true;
        requestLastKnownLocation(host);
        fusedLocationClient.requestLocationUpdates(
                buildLocationRequest(),
                locationCallback,
                Looper.getMainLooper()
        );
    }

    // Stops active location updates.
    void stopTracking() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        tracking = false;
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocationLayer(GoogleMap googleMap) {
        if (googleMap != null) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void ensureLocationCallback(Host host) {
        if (locationCallback != null) {
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location == null) {
                    return;
                }
                host.onLiveLocationAvailable(toLatLng(location));
            }
        };
    }

    private void requestLastKnownLocation(Host host) {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                host.onLocationUnavailable();
                return;
            }
            host.onInitialLocationAvailable(toLatLng(location));
        });
    }

    private LocationRequest buildLocationRequest() {
        return new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000)
                .build();
    }

    private LatLng toLatLng(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }
}
