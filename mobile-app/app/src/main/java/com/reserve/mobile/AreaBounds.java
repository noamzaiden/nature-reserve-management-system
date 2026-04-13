package com.reserve.mobile;

// Defines a simple rectangular area using min/max coordinates.
public final class AreaBounds {

    private final double minLatitude;
    private final double maxLatitude;
    private final double minLongitude;
    private final double maxLongitude;

    // Creates a simple rectangular boundary from min/max latitude and longitude.
    public AreaBounds(double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
    }

    // Returns the southern edge latitude.
    public double getMinLatitude() {
        return minLatitude;
    }

    // Returns the northern edge latitude.
    public double getMaxLatitude() {
        return maxLatitude;
    }

    // Returns the western edge longitude.
    public double getMinLongitude() {
        return minLongitude;
    }

    // Returns the eastern edge longitude.
    public double getMaxLongitude() {
        return maxLongitude;
    }

    // Checks whether a point is inside this rectangle (including edges).
    public boolean contains(double latitude, double longitude) {
        return latitude >= minLatitude && latitude <= maxLatitude
                && longitude >= minLongitude && longitude <= maxLongitude;
    }
}
