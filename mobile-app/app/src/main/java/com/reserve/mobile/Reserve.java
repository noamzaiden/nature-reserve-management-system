package com.reserve.mobile;

public final class Reserve {

    private final long id;
    private final String displayName;
    private final double centerLatitude;
    private final double centerLongitude;
    private final AreaBounds areaBounds;

    public Reserve(long id, String fallbackName, String displayName,
                   double centerLatitude, double centerLongitude, AreaBounds areaBounds) {
        this.id = id;
        this.displayName = displayName == null || displayName.isEmpty() ? fallbackName : displayName;
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

    @Override
    public String toString() {
        return displayName;
    }
}
