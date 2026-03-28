ALTER TABLE reserves
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS osm_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS osm_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS center_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS center_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS polygon_geojson TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS reserves_osm_source_uk
    ON reserves (osm_type, osm_id)
    WHERE osm_type IS NOT NULL AND osm_id IS NOT NULL;
