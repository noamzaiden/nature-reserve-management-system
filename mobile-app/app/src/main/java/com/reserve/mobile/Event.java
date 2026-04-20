package com.reserve.mobile;

import com.google.android.gms.maps.model.LatLng;

public final class Event {

    private final long reserveId;
    private final String type;
    private final String priority;
    private final String description;
    private final double latitude;
    private final double longitude;

    public Event(long reserveId, String type, String priority, String description,
                 double latitude, double longitude) {
        this.reserveId = reserveId;
        this.type = type;
        this.priority = priority;
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

    public String getPriority() {
        return priority;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFire() {
        return type != null && "FIRE".equalsIgnoreCase(type.trim());
    }

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    public LatLng latLng() {
        return new LatLng(latitude, longitude);
    }
}
