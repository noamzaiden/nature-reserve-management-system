package com.reserve.mobile;

import com.google.android.gms.maps.model.LatLng;

public final class Poi {

    private final long reserveId;
    private final String type;
    private final String name;
    private final String description;
    private final double latitude;
    private final double longitude;

    public Poi(long reserveId, String type, String name, String description,
               double latitude, double longitude) {
        this.reserveId = reserveId;
        this.type = type == null || type.isEmpty() ? "POI" : type;
        this.name = name == null || name.isEmpty() ? this.type : name;
        this.description = description == null ? "" : description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public long getReserveId() {
        return reserveId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    public LatLng latLng() {
        return new LatLng(latitude, longitude);
    }
}
