package com.noam.fleetcommand.reserves.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class AreaDto {

    @NotNull
    private Double minLatitude;

    @NotNull
    private Double maxLatitude;

    @NotNull
    private Double minLongitude;

    @NotNull
    private Double maxLongitude;

    public AreaDto() {
    }

    public AreaDto(Double minLatitude, Double maxLatitude, Double minLongitude, Double maxLongitude) {
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AreaDto areaDto = (AreaDto) o;
        return Objects.equals(minLatitude, areaDto.minLatitude) &&
                Objects.equals(maxLatitude, areaDto.maxLatitude) &&
                Objects.equals(minLongitude, areaDto.minLongitude) &&
                Objects.equals(maxLongitude, areaDto.maxLongitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minLatitude, maxLatitude, minLongitude, maxLongitude);
    }

    @Override
    public String toString() {
        return "AreaDto{" +
                "minLatitude=" + minLatitude +
                ", maxLatitude=" + maxLatitude +
                ", minLongitude=" + minLongitude +
                ", maxLongitude=" + maxLongitude +
                '}';
    }
}
