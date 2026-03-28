CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

ALTER TABLE events
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN description VARCHAR(1000),
    ADD COLUMN latitude DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN longitude DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN assigned_user_id BIGINT,
    ADD COLUMN updated_at TIMESTAMP,
    ADD COLUMN closed_at TIMESTAMP;

ALTER TABLE events
    ADD CONSTRAINT events_type_chk CHECK (type IN ('FIRE', 'BLOCKAGE', 'OTHER')),
    ADD CONSTRAINT events_latitude_chk CHECK (latitude BETWEEN -90 AND 90),
    ADD CONSTRAINT events_longitude_chk CHECK (longitude BETWEEN -180 AND 180),
    ADD CONSTRAINT events_assigned_user_fk FOREIGN KEY (assigned_user_id) REFERENCES users(id);

ALTER TABLE events
    ALTER COLUMN latitude DROP DEFAULT,
    ALTER COLUMN longitude DROP DEFAULT,
    ALTER COLUMN type DROP DEFAULT;
