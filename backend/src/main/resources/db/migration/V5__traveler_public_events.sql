ALTER TABLE events
    ADD COLUMN origin VARCHAR(24) NOT NULL DEFAULT 'MANAGER',
    ADD COLUMN reporter_name VARCHAR(120),
    ADD COLUMN published_to_travelers BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE events
    ADD CONSTRAINT events_origin_chk CHECK (origin IN ('MANAGER', 'TRAVELER_REPORT'));

CREATE TABLE event_media (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    media_url VARCHAR(512) NOT NULL,
    media_type VARCHAR(16) NOT NULL,
    original_filename VARCHAR(255)
);
