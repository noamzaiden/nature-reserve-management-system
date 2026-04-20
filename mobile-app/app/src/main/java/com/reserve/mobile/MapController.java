package com.reserve.mobile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapController {

    private static final LatLng DEFAULT_MAP_CENTER = new LatLng(31.4117, 35.0818);
    private static final float DEFAULT_MAP_ZOOM = 7f;
    private static final int POI_ICON_SIZE_MIN_PX = 64;
    private static final int POI_ICON_SIZE_LOW_PX = 80;
    private static final int POI_ICON_SIZE_MEDIUM_PX = 96;
    private static final int POI_ICON_SIZE_LARGE_PX = 112;
    private static final int POI_ICON_SIZE_MAX_PX = 128;
    private static final int ACTIVE_RESERVE_STROKE = Color.parseColor("#537B5D");
    private static final int INACTIVE_RESERVE_STROKE = Color.parseColor("#8CA991");
    private static final List<PatternItem> RESERVE_STROKE_PATTERN = Arrays.asList(new Dash(24f), new Gap(14f));

    private final Context context;
    private final Map<String, BitmapDescriptor> poiIconCache = new HashMap<>();
    private GoogleMap googleMap;
    private boolean showingPois = true;
    private boolean showingHazards = true;
    private int lastPoiIconSizePx = -1;

    public MapController(Context context) {
        this.context = context;
    }

    public void attachMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM));
        applyMapStyle();
    }

    public boolean isShowingPois() {
        return showingPois;
    }

    public void setShowPois(boolean showingPois) {
        this.showingPois = showingPois;
        applyMapStyle();
    }

    public boolean isShowingHazards() {
        return showingHazards;
    }

    public void setShowHazards(boolean showingHazards) {
        this.showingHazards = showingHazards;
    }

    public boolean shouldRefreshAfterCameraIdle() {
        return googleMap != null && currentPoiIconSizePx() != lastPoiIconSizePx;
    }

    @SuppressLint("MissingPermission")
    public void refresh(List<Reserve> reserves, List<Poi> pois, List<Event> hazards,
                        Reserve currentReserve, boolean hasLocationPermission) {
        if (googleMap == null) {
            return;
        }

        lastPoiIconSizePx = currentPoiIconSizePx();
        googleMap.clear();
        applyMapStyle();

        if (hasLocationPermission) {
            googleMap.setMyLocationEnabled(true);
        }

        drawReserveBoundsForAll(reserves, currentReserve);
        drawPoisIfEnabled(pois);
        drawHazardsIfEnabled(hazards);
    }

    private void drawReserveBoundsForAll(List<Reserve> reserves, Reserve currentReserve) {
        for (Reserve reserve : reserves) {
            drawReserveBounds(reserve, currentReserve);
        }
    }

    private void drawHazardsIfEnabled(List<Event> hazards) {
        if (!showingHazards) {
            return;
        }
        for (Event hazard : hazards) {
            drawHazardMarker(hazard);
        }
    }

    private void drawPoisIfEnabled(List<Poi> pois) {
        if (!showingPois) {
            return;
        }
        for (Poi poi : pois) {
            drawPoiMarker(poi);
        }
    }

    private void drawReserveBounds(Reserve reserve, Reserve currentReserve) {
        AreaBounds areaBounds = reserve.getAreaBounds();
        if (areaBounds == null || googleMap == null) {
            return;
        }

        int strokeColor = reserve == currentReserve ? ACTIVE_RESERVE_STROKE : INACTIVE_RESERVE_STROKE;

        googleMap.addPolygon(new PolygonOptions()
                .add(
                        new LatLng(areaBounds.getMinLatitude(), areaBounds.getMinLongitude()),
                        new LatLng(areaBounds.getMinLatitude(), areaBounds.getMaxLongitude()),
                        new LatLng(areaBounds.getMaxLatitude(), areaBounds.getMaxLongitude()),
                        new LatLng(areaBounds.getMaxLatitude(), areaBounds.getMinLongitude())
                )
                .strokeWidth(reserve == currentReserve ? 5f : 4f)
                .strokeColor(strokeColor)
                .strokePattern(RESERVE_STROKE_PATTERN)
                .fillColor(Color.TRANSPARENT));
    }

    private void drawHazardMarker(Event hazard) {
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
                .icon(hazardIconDescriptor(hazard)));
    }

    private void drawPoiMarker(Poi poi) {
        if (!poi.hasCoordinates() || googleMap == null) {
            return;
        }

        String snippet = poi.getDescription().isEmpty()
                ? poi.getType()
                : poi.getType() + ": " + poi.getDescription();

        googleMap.addMarker(new MarkerOptions()
                .position(poi.latLng())
                .title(poi.getName())
                .snippet(snippet)
                .icon(poiIconDescriptor(poi.getType())));
    }

    private void applyMapStyle() {
        if (googleMap == null) {
            return;
        }

        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(
                context,
                showingPois ? R.raw.map_style_nature : R.raw.map_style_nature_no_poi
        ));
    }

    private float priorityHue(String priority) {
        if ("HIGH".equalsIgnoreCase(priority)) {
            return BitmapDescriptorFactory.HUE_RED;
        }
        if ("MEDIUM".equalsIgnoreCase(priority)) {
            return BitmapDescriptorFactory.HUE_ORANGE;
        }
        return BitmapDescriptorFactory.HUE_YELLOW;
    }

    private BitmapDescriptor hazardIconDescriptor(Event hazard) {
        if (!hazard.isFire()) {
            return BitmapDescriptorFactory.defaultMarker(priorityHue(hazard.getPriority()));
        }

        int iconSizePx = currentPoiIconSizePx();
        String cacheKey = "hazard-fire:" + iconSizePx;
        BitmapDescriptor cached = poiIconCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BitmapDescriptor descriptor = createPoiIconDescriptor(R.drawable.poi_fire, iconSizePx);
        if (descriptor == null) {
            return BitmapDescriptorFactory.defaultMarker(priorityHue(hazard.getPriority()));
        }

        poiIconCache.put(cacheKey, descriptor);
        return descriptor;
    }

    private BitmapDescriptor poiIconDescriptor(String type) {
        String normalizedType = normalizePoiType(type);
        int iconSizePx = currentPoiIconSizePx();
        String cacheKey = normalizedType + ":" + iconSizePx;
        int resourceId = poiIconResourceId(normalizedType);
        if (resourceId == 0) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
        }

        BitmapDescriptor cached = poiIconCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BitmapDescriptor descriptor = createPoiIconDescriptor(resourceId, iconSizePx);
        if (descriptor == null) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE);
        }

        poiIconCache.put(cacheKey, descriptor);
        return descriptor;
    }

    private int poiIconResourceId(String normalizedType) {
        if ("fire".equals(normalizedType) || "fire point".equals(normalizedType) || "campfire".equals(normalizedType)) {
            return R.drawable.poi_fire;
        }
        if ("parking".equals(normalizedType)) {
            return R.drawable.poi_parking;
        }
        if ("information desk".equals(normalizedType)) {
            return R.drawable.poi_information_desk;
        }
        if ("viewpoint".equals(normalizedType)
                || "lookout".equals(normalizedType)
                || "scenic view".equals(normalizedType)
                || "scenicview".equals(normalizedType)
                || "scenic lookout".equals(normalizedType)) {
            return R.drawable.poi_viewpoint;
        }
        if ("first aid".equals(normalizedType)) {
            return R.drawable.poi_first_aid;
        }
        if ("toilet".equals(normalizedType)
                || "restroom".equals(normalizedType)
                || "bathroom".equals(normalizedType)
                || "wc".equals(normalizedType)) {
            return R.drawable.poi_restroom;
        }
        return 0;
    }

    private BitmapDescriptor createPoiIconDescriptor(int resourceId, int iconSizePx) {
        Drawable drawable = AppCompatResources.getDrawable(context, resourceId);
        if (drawable == null) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(
                iconSizePx,
                iconSizePx,
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, iconSizePx, iconSizePx);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private int currentPoiIconSizePx() {
        if (googleMap == null) {
            return POI_ICON_SIZE_MEDIUM_PX;
        }

        float zoom = googleMap.getCameraPosition().zoom;
        if (zoom < 9f) {
            return POI_ICON_SIZE_MIN_PX;
        }
        if (zoom < 11f) {
            return POI_ICON_SIZE_LOW_PX;
        }
        if (zoom < 13f) {
            return POI_ICON_SIZE_MEDIUM_PX;
        }
        if (zoom < 15f) {
            return POI_ICON_SIZE_LARGE_PX;
        }
        return POI_ICON_SIZE_MAX_PX;
    }

    private String normalizePoiType(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.US);
    }
}
