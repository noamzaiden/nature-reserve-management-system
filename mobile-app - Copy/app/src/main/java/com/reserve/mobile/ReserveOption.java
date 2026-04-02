package com.reserve.mobile;

import com.google.android.gms.maps.model.LatLng;

public class ReserveOption {

    private final long id;
    private final String displayName;
    private final double centerLatitude;
    private final double centerLongitude;
    private final AreaBounds areaBounds;

    public ReserveOption(long id, String name, String displayName,
                         double centerLatitude, double centerLongitude, AreaBounds areaBounds) {
        this.id = id;
        this.displayName = displayName == null || displayName.isEmpty() ? name : displayName;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.areaBounds = areaBounds;
    }

    public long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public AreaBounds getAreaBounds() {
        return areaBounds;
    }

    public boolean hasCenterPoint() {
        return !Double.isNaN(centerLatitude) && !Double.isNaN(centerLongitude);
    }

    public LatLng centerLatLng() {
        return new LatLng(centerLatitude, centerLongitude);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
