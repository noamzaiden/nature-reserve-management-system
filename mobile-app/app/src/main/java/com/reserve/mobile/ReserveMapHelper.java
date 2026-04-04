package com.reserve.mobile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.List;
import java.util.Locale;

public class ReserveMapHelper {

    private static final LatLng DEFAULT_MAP_CENTER = new LatLng(31.4117, 35.0818);
    private static final float DEFAULT_MAP_ZOOM = 7f;

    private final Context context;
    private GoogleMap googleMap;
    private boolean showingPois = true;
    private boolean showingHazards = true;

    // Creates helper that owns map rendering state for layers.
    public ReserveMapHelper(Context context) {
        this.context = context;
    }

    // Attaches the GoogleMap instance and applies initial camera/style.
    public void attachMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM));
        applyMapStyle();
    }

    // Returns whether reserve center POI markers are visible.
    public boolean isShowingPois() {
        return showingPois;
    }

    // Enables/disables POI markers and corresponding map style.
    public void setShowPois(boolean showingPois) {
        this.showingPois = showingPois;
        applyMapStyle();
    }

    // Returns whether hazard markers are visible.
    public boolean isShowingHazards() {
        return showingHazards;
    }

    // Enables/disables hazard marker rendering.
    public void setShowHazards(boolean showingHazards) {
        this.showingHazards = showingHazards;
    }

    @SuppressLint("MissingPermission")
    // Clears and redraws all map layers (bounds, POIs, hazards).
    public void refresh(List<Reserve> reserves, List<PublicEvent> hazards,
                        Reserve currentReserve, boolean hasLocationPermission) {
        if (googleMap == null) {
            return;
        }

        googleMap.clear();
        applyMapStyle();

        if (hasLocationPermission) {
            googleMap.setMyLocationEnabled(true);
        }

        for (Reserve reserve : reserves) {
            drawReserveBounds(reserve, currentReserve);
        }

        if (showingHazards) {
            for (PublicEvent hazard : hazards) {
                drawHazardMarker(hazard);
            }
        }
    }

    // Draws one reserve boundary polygon and highlights current reserve.
    private void drawReserveBounds(Reserve reserve, Reserve currentReserve) {
        AreaBounds areaBounds = reserve.getAreaBounds();
        if (areaBounds == null || googleMap == null) {
            return;
        }

        int strokeColor = reserve == currentReserve ? Color.parseColor("#537B5D") : Color.parseColor("#8CA991");
        int fillColor = reserve == currentReserve ? Color.parseColor("#505F8D67") : Color.parseColor("#268CA991");

        googleMap.addPolygon(new PolygonOptions()
                .add(
                        new LatLng(areaBounds.getMinLatitude(), areaBounds.getMinLongitude()),
                        new LatLng(areaBounds.getMinLatitude(), areaBounds.getMaxLongitude()),
                        new LatLng(areaBounds.getMaxLatitude(), areaBounds.getMaxLongitude()),
                        new LatLng(areaBounds.getMaxLatitude(), areaBounds.getMinLongitude())
                )
                .strokeWidth(4f)
                .strokeColor(strokeColor)
                .fillColor(fillColor));
    }


    // Draws one hazard marker with color based on priority.
    private void drawHazardMarker(PublicEvent hazard) {
        if (!hazard.hasCoordinates() || googleMap == null) {
            return;
        }

        String snippet = hazard.getDescription().isEmpty()
                ? context.getString(R.string.hazard_snippet, hazard.getPriority().toLowerCase(Locale.US))
                : hazard.getDescription();

        googleMap.addMarker(new MarkerOptions()
                .position(hazard.latLng())
                .title(hazard.getType())
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(priorityHue(hazard.getPriority()))));
    }

    // Applies map style resource depending on POI toggle.
    private void applyMapStyle() {
        if (googleMap == null) {
            return;
        }

        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                context,
                showingPois ? R.raw.map_style_nature : R.raw.map_style_nature_no_poi
        ));
    }

    // Maps priority text to default Google marker hue.
    private float priorityHue(String priority) {
        if ("HIGH".equalsIgnoreCase(priority)) {
            return BitmapDescriptorFactory.HUE_RED;
        }
        if ("MEDIUM".equalsIgnoreCase(priority)) {
            return BitmapDescriptorFactory.HUE_ORANGE;
        }
        return BitmapDescriptorFactory.HUE_YELLOW;
    }
}
