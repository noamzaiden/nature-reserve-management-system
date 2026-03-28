package com.noam.fleetcommand.requests;

import com.noam.fleetcommand.users.User;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reserve_creation_requests")
@NoArgsConstructor
public class ReserveCreationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requested_reserve_name", nullable = false, length = 255)
    private String requestedReserveName;

    @Column(name = "request_message", nullable = false, length = 2000)
    private String requestMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReserveRequestStatus status = ReserveRequestStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Long getId() {
        return id;
    }

    public String getRequestedReserveName() {
        return requestedReserveName;
    }

    public void setRequestedReserveName(String requestedReserveName) {
        this.requestedReserveName = requestedReserveName;
    }

    public String getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(String requestMessage) {
        this.requestMessage = requestMessage;
    }

    public ReserveRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ReserveRequestStatus status) {
        this.status = status;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
