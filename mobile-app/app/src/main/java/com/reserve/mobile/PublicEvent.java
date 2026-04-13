package com.reserve.mobile;

import com.google.android.gms.maps.model.LatLng;

public final class PublicEvent {

    private final long reserveId;
    private final String type;
    private final String priority;
    private final String description;
    private final double latitude;
    private final double longitude;

    // Stores one public event/hazard record returned by the backend.
    public PublicEvent(long reserveId, String type, String priority, String description,
                       double latitude, double longitude) {
        this.reserveId = reserveId;
        this.type = type;
        this.priority = priority;
        this.description = description == null ? "" : description;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Returns the reserve id this event belongs to.
    public long getReserveId() {
        return reserveId;
    }

    // Returns the event type (FIRE, BLOCKAGE, OTHER...).
    public String getType() {
        return type;
    }

    // Returns event priority used for marker color.
    public String getPriority() {
        return priority;
    }

    // Returns the human-readable event description.
    public String getDescription() {
        return description;
    }

    // Tells whether this event has valid coordinates.
    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    // Converts stored coordinates to Google Maps LatLng.
    public LatLng latLng() {
        return new LatLng(latitude, longitude);
    }
}
