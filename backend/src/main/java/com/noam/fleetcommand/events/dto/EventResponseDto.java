package com.noam.fleetcommand.events.dto;

import com.noam.fleetcommand.events.EventPriority;
import com.noam.fleetcommand.events.EventStatus;
import com.noam.fleetcommand.events.EventType;
import com.noam.fleetcommand.events.EventOrigin;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public class EventResponseDto {
    private Long id;
    private Long reserveId;
    private String reserveName;
    private EventPriority priority;
    private EventStatus status;
    private EventType type;
    private EventOrigin origin;
    private String description;
    private String reporterName;
    private boolean publishedToTravelers;
    private Double latitude;
    private Double longitude;
    private Long assignedUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private List<EventMediaDto> media;

    public EventResponseDto() {
    }

    public EventResponseDto(Long id, Long reserveId, String reserveName, EventPriority priority, EventStatus status, EventType type,
                            EventOrigin origin, String description, String reporterName, boolean publishedToTravelers,
                            Double latitude, Double longitude, Long assignedUserId,
                            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime closedAt,
                            List<EventMediaDto> media) {
        this.id = id;
        this.reserveId = reserveId;
        this.reserveName = reserveName;
        this.priority = priority;
        this.status = status;
        this.type = type;
        this.origin = origin;
        this.description = description;
        this.reporterName = reporterName;
        this.publishedToTravelers = publishedToTravelers;
        this.latitude = latitude;
        this.longitude = longitude;
        this.assignedUserId = assignedUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.closedAt = closedAt;
        this.media = media;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventResponseDto that = (EventResponseDto) o;
        return Objects.equals(id, that.id)
                && Objects.equals(reserveId, that.reserveId)
                && Objects.equals(reserveName, that.reserveName)
                && priority == that.priority
                && status == that.status
                && type == that.type
                && origin == that.origin
                && Objects.equals(description, that.description)
                && Objects.equals(reporterName, that.reporterName)
                && publishedToTravelers == that.publishedToTravelers
                && Objects.equals(latitude, that.latitude)
                && Objects.equals(longitude, that.longitude)
                && Objects.equals(assignedUserId, that.assignedUserId)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt)
                && Objects.equals(closedAt, that.closedAt)
                && Objects.equals(media, that.media);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reserveId, reserveName, priority, status, type, origin, description, reporterName,
                publishedToTravelers, latitude, longitude, assignedUserId, createdAt, updatedAt, closedAt, media);
    }

    @Override
    public String toString() {
        return "EventResponseDto{" +
                "id=" + id +
                ", reserveId=" + reserveId +
                ", reserveName='" + reserveName + '\'' +
                ", priority=" + priority +
                ", status=" + status +
                ", type=" + type +
                ", origin=" + origin +
                ", description='" + description + '\'' +
                ", reporterName='" + reporterName + '\'' +
                ", publishedToTravelers=" + publishedToTravelers +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", assignedUserId=" + assignedUserId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", closedAt=" + closedAt +
                ", media=" + media +
                '}';
    }
}
