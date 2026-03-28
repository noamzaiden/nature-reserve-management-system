package com.noam.fleetcommand.events;

import com.noam.fleetcommand.reserves.Reserve;
import com.noam.fleetcommand.users.User;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserve_id", nullable = false)
    private Reserve reserve;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventOrigin origin = EventOrigin.MANAGER;

    @Column(length = 1000)
    private String description;

    @Column(name = "reporter_name", length = 120)
    private String reporterName;

    @Column(name = "published_to_travelers", nullable = false)
    private boolean publishedToTravelers;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventMedia> media = new ArrayList<>();

    public Event(Reserve reserve, EventPriority priority, EventStatus status, EventType type, String description,
                 Double latitude, Double longitude, User assignedUser) {
        this.reserve = reserve;
        this.priority = priority;
        this.status = status;
        this.type = type;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.assignedUser = assignedUser;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Reserve getReserve() { return reserve; }
    public void setReserve(Reserve reserve) { this.reserve = reserve; }
    public EventPriority getPriority() { return priority; }
    public void setPriority(EventPriority priority) { this.priority = priority; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public EventOrigin getOrigin() { return origin; }
    public void setOrigin(EventOrigin origin) { this.origin = origin; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public boolean isPublishedToTravelers() { return publishedToTravelers; }
    public void setPublishedToTravelers(boolean publishedToTravelers) { this.publishedToTravelers = publishedToTravelers; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public User getAssignedUser() { return assignedUser; }
    public void setAssignedUser(User assignedUser) { this.assignedUser = assignedUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public List<EventMedia> getMedia() { return media; }
    public void setMedia(List<EventMedia> media) { this.media = media; }

    public void addMedia(EventMedia mediaItem) {
        media.add(mediaItem);
        mediaItem.setEvent(this);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", priority=" + priority +
                ", status=" + status +
                ", type=" + type +
                ", origin=" + origin +
                ", publishedToTravelers=" + publishedToTravelers +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", closedAt=" + closedAt +
                '}';
    }
}
