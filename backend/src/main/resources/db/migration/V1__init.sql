CREATE TABLE reserves (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100),
    region VARCHAR(32),
    min_latitude DOUBLE PRECISION NOT NULL,
    max_latitude DOUBLE PRECISION NOT NULL,
    min_longitude DOUBLE PRECISION NOT NULL,
    max_longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT reserves_name_uk UNIQUE (name),
    CONSTRAINT reserves_lat_bbox_chk CHECK (min_latitude < max_latitude),
    CONSTRAINT reserves_lon_bbox_chk CHECK (min_longitude < max_longitude)
);

CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    reserve_id BIGINT NOT NULL REFERENCES reserves(id),
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT events_priority_chk CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT events_status_chk CHECK (status IN ('OPEN', 'IN_PROGRESS', 'CLOSED'))
);
