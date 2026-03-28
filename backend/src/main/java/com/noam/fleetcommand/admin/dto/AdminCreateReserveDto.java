package com.noam.fleetcommand.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AdminCreateReserveDto {

    @NotBlank
    private String name;

    @NotBlank
    private String region;

    @NotNull
    private Long managerUserId;

    private Long reserveRequestId;

    @NotNull
    @Min(-90)
    @Max(90)
    private Double minLatitude;

    @NotNull
    @Min(-90)
    @Max(90)
    private Double maxLatitude;

    @NotNull
    @Min(-180)
    @Max(180)
    private Double minLongitude;

    @NotNull
    @Min(-180)
    @Max(180)
    private Double maxLongitude;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Long getManagerUserId() {
        return managerUserId;
    }

    public void setManagerUserId(Long managerUserId) {
        this.managerUserId = managerUserId;
    }

    public Long getReserveRequestId() {
        return reserveRequestId;
    }

    public void setReserveRequestId(Long reserveRequestId) {
        this.reserveRequestId = reserveRequestId;
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
}
