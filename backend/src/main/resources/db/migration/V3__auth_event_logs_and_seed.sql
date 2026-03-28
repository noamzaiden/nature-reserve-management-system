ALTER TABLE users
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'INSPECTOR';

ALTER TABLE users
    ADD CONSTRAINT users_email_uk UNIQUE (email),
    ADD CONSTRAINT users_role_chk CHECK (role IN ('ADMIN', 'INSPECTOR'));

CREATE TABLE admin_reserves (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reserve_id BIGINT NOT NULL REFERENCES reserves(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, reserve_id)
);

CREATE TABLE event_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    action VARCHAR(100) NOT NULL,
    note VARCHAR(1000),
    performed_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO reserves (name, region, min_latitude, max_latitude, min_longitude, max_longitude)
VALUES
('Carmel Reserve', 'North', 32.6500, 32.8100, 34.9200, 35.0800),
('Ein Gedi Reserve', 'Dead Sea', 31.4100, 31.5100, 35.3500, 35.4600),
('Hula Reserve', 'Galilee', 33.0600, 33.1200, 35.5900, 35.6600)
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (name, email, password_hash, role)
VALUES
('Admin North', 'admin@reserve.local', 'ChangeMe123!', 'ADMIN'),
('Inspector One', 'inspector@reserve.local', 'ChangeMe123!', 'INSPECTOR')
ON CONFLICT (email) DO NOTHING;

INSERT INTO admin_reserves (user_id, reserve_id)
SELECT u.id, r.id
FROM users u
JOIN reserves r ON r.name IN ('Carmel Reserve', 'Hula Reserve')
WHERE u.email = 'admin@reserve.local'
ON CONFLICT DO NOTHING;
