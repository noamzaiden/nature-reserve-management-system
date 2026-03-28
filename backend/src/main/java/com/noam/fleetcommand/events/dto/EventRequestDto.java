package com.noam.fleetcommand.events.dto;

import com.noam.fleetcommand.events.EventPriority;
import com.noam.fleetcommand.events.EventStatus;
import com.noam.fleetcommand.events.EventType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class EventRequestDto {

    @NotNull(message = "Reserve ID is required")
    private Long reserveId;

    @NotNull(message = "Priority is required")
    private EventPriority priority;

    @NotNull(message = "Status is required")
    private EventStatus status;

    @NotNull(message = "Type is required")
    private EventType type;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180")
    private Double longitude;

    private Boolean publishedToTravelers;

    private Long assignedUserId;

    public EventRequestDto() {
    }

    public EventRequestDto(Long reserveId, EventPriority priority, EventStatus status, EventType type,
                           String description, Double latitude, Double longitude, Boolean publishedToTravelers, Long assignedUserId) {
        this.reserveId = reserveId;
        this.priority = priority;
        this.status = status;
        this.type = type;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.publishedToTravelers = publishedToTravelers;
        this.assignedUserId = assignedUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventRequestDto that = (EventRequestDto) o;
        return Objects.equals(reserveId, that.reserveId)
                && priority == that.priority
                && status == that.status
                && type == that.type
                && Objects.equals(description, that.description)
                && Objects.equals(latitude, that.latitude)
                && Objects.equals(longitude, that.longitude)
                && Objects.equals(publishedToTravelers, that.publishedToTravelers)
                && Objects.equals(assignedUserId, that.assignedUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reserveId, priority, status, type, description, latitude, longitude, publishedToTravelers, assignedUserId);
    }

    @Override
    public String toString() {
        return "EventRequestDto{" +
                "reserveId=" + reserveId +
                ", priority=" + priority +
                ", status=" + status +
                ", type=" + type +
                ", description='" + description + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", publishedToTravelers=" + publishedToTravelers +
                ", assignedUserId=" + assignedUserId +
                '}';
    }
}
