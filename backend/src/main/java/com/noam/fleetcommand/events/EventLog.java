package com.noam.fleetcommand.events;

import com.noam.fleetcommand.users.User;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_logs")
@NoArgsConstructor
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 1000)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    private User performedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public EventLog(Event event, String action, String note, User performedBy) {
        this.event = event;
        this.action = action;
        this.note = note;
        this.performedBy = performedBy;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public String getAction() { return action; }
    public String getNote() { return note; }
    public User getPerformedBy() { return performedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
