package com.noam.fleetcommand.reserves.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
public class ReserveResponseDto {
    private Long id;
    private String name;
    private String region;
    private String displayName;
    private AreaDto area;
    private Long managerUserId;
    private Double centerLatitude;
    private Double centerLongitude;
    private LocalDateTime createdAt;

    public ReserveResponseDto() {
    }

    public ReserveResponseDto(Long id, String name, String region, String displayName, AreaDto area, Long managerUserId, Double centerLatitude, Double centerLongitude, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.displayName = displayName;
        this.area = area;
        this.managerUserId = managerUserId;
        this.centerLatitude = centerLatitude;
        this.centerLongitude = centerLongitude;
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReserveResponseDto that = (ReserveResponseDto) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(region, that.region) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(area, that.area) &&
                Objects.equals(managerUserId, that.managerUserId) &&
                Objects.equals(centerLatitude, that.centerLatitude) &&
                Objects.equals(centerLongitude, that.centerLongitude) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, region, displayName, area, managerUserId, centerLatitude, centerLongitude, createdAt);
    }

    @Override
    public String toString() {
        return "ReserveResponseDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", region='" + region + '\'' +
                ", displayName='" + displayName + '\'' +
                ", area=" + area +
                ", managerUserId=" + managerUserId +
                ", centerLatitude=" + centerLatitude +
                ", centerLongitude=" + centerLongitude +
                ", createdAt=" + createdAt +
                '}';
    }
}
