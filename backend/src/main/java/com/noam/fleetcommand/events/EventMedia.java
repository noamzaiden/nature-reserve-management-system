package com.noam.fleetcommand.events;

import jakarta.persistence.*;

@Entity
@Table(name = "event_media")
public class EventMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "media_url", nullable = false, length = 512)
    private String mediaUrl;

    @Column(name = "media_type", nullable = false, length = 16)
    private String mediaType;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    public EventMedia() {
    }

    public EventMedia(String mediaUrl, String mediaType, String originalFilename) {
        this.mediaUrl = mediaUrl;
        this.mediaType = mediaType;
        this.originalFilename = originalFilename;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }
}
