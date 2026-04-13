package com.reserve.mobile;

public final class Reserve {

    private final long id;
    private final String displayName;
    private final double centerLatitude;
    private final double centerLongitude;
    private final AreaBounds areaBounds;

    // Builds one reserve object for map and spinner usage.
    public Reserve(long id, String name, String displayName,
                         double centerLatitude, double centerLongitude, AreaBounds areaBounds) {
        this.id = id;
        this.displayName = displayName == null || displayName.isEmpty() ? name : displayName;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.areaBounds = areaBounds;
    }

    // Returns unique reserve id from backend.
    public long getId() {
        return id;
    }

    // Returns the user-facing reserve name.
    public String getDisplayName() {
        return displayName;
    }

    // Returns center latitude used for nearest-distance checks.
    public double getCenterLatitude() {
        return centerLatitude;
    }

    // Returns center longitude used for nearest-distance checks.
    public double getCenterLongitude() {
        return centerLongitude;
    }

    // Returns rectangular reserve boundaries, if available.
    public AreaBounds getAreaBounds() {
        return areaBounds;
    }

    // Tells whether reserve has a valid center coordinate.
    public boolean hasCenterPoint() {
        return !Double.isNaN(centerLatitude) && !Double.isNaN(centerLongitude);
    }


    // Spinner uses this text when rendering list items.
    @Override
    public String toString() {
        return displayName;
    }
}

