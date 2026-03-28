package com.noam.fleetcommand.reserves;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
public class Area {

    @Column(name = "min_latitude")
    private Double minLatitude;

    @Column(name = "max_latitude")
    private Double maxLatitude;

    @Column(name = "min_longitude")
    private Double minLongitude;

    @Column(name = "max_longitude")
    private Double maxLongitude;

    public Area(Double minLatitude, Double maxLatitude, Double minLongitude, Double maxLongitude) {
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
    }

    public Double getMinLatitude() {
        return minLatitude;
    }

    public void setMinLatitude(Double minLatitude) {
        this.minLatitude = minLatitude;
    }

    public Double getMaxLatitude() {
        return maxLatitude;
    }

    public void setMaxLatitude(Double maxLatitude) {
        this.maxLatitude = maxLatitude;
    }

    public Double getMinLongitude() {
        return minLongitude;
    }

    public void setMinLongitude(Double minLongitude) {
        this.minLongitude = minLongitude;
    }

    public Double getMaxLongitude() {
        return maxLongitude;
    }

    public void setMaxLongitude(Double maxLongitude) {
        this.maxLongitude = maxLongitude;
    }

    public boolean contains(Double latitude, Double longitude) {
        return latitude != null && longitude != null
                && latitude >= minLatitude && latitude <= maxLatitude
                && longitude >= minLongitude && longitude <= maxLongitude;
    }

    public boolean overlaps(Area other) {
        if (other == null) {
            return false;
        }

        return !(maxLatitude < other.minLatitude
                || minLatitude > other.maxLatitude
                || maxLongitude < other.minLongitude
                || minLongitude > other.maxLongitude);
    }
}
