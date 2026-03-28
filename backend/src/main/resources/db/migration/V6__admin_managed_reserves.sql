ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_role_chk;

UPDATE users
SET role = 'MANAGER'
WHERE role = 'INSPECTOR';

UPDATE users
SET role = 'MANAGER'
WHERE email <> 'admin@reserve.local';

ALTER TABLE users
    ADD CONSTRAINT users_role_chk CHECK (role IN ('ADMIN', 'MANAGER'));

DELETE FROM event_media;
DELETE FROM event_logs;
DELETE FROM events;
DROP TABLE IF EXISTS admin_reserves;
DELETE FROM reserves;

CREATE TABLE reserve_creation_requests (
    id BIGSERIAL PRIMARY KEY,
    requested_reserve_name VARCHAR(255) NOT NULL,
    request_message VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    requested_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP NULL,
    CONSTRAINT reserve_creation_requests_status_chk CHECK (status IN ('OPEN', 'APPROVED', 'REJECTED'))
);

INSERT INTO users (name, email, password_hash, role)
VALUES ('System Admin', 'admin@reserve.local', 'ChangeMe123!', 'ADMIN')
ON CONFLICT (email) DO UPDATE
SET name = EXCLUDED.name,
    password_hash = EXCLUDED.password_hash,
    role = 'ADMIN';
