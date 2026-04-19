CREATE TABLE reserve_poi_types (
    id BIGSERIAL PRIMARY KEY,
    reserve_id BIGINT NOT NULL REFERENCES reserves (id) ON DELETE CASCADE,
    name VARCHAR(80) NOT NULL,
    system_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reserve_poi_type_name UNIQUE (reserve_id, name)
);

CREATE TABLE reserve_pois (
    id BIGSERIAL PRIMARY KEY,
    reserve_id BIGINT NOT NULL REFERENCES reserves (id) ON DELETE CASCADE,
    type_id BIGINT NOT NULL REFERENCES reserve_poi_types (id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reserve_poi_types_reserve_id ON reserve_poi_types (reserve_id);
CREATE INDEX idx_reserve_pois_reserve_id ON reserve_pois (reserve_id);
CREATE INDEX idx_reserve_pois_type_id ON reserve_pois (type_id);
