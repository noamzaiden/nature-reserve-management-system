TRUNCATE TABLE event_media RESTART IDENTITY CASCADE;
TRUNCATE TABLE event_logs RESTART IDENTITY CASCADE;
TRUNCATE TABLE events RESTART IDENTITY CASCADE;
TRUNCATE TABLE reserve_creation_requests RESTART IDENTITY CASCADE;
TRUNCATE TABLE reserves RESTART IDENTITY CASCADE;

DELETE FROM users
WHERE email <> 'admin@reserve.local';

INSERT INTO users (name, email, password_hash, role)
VALUES ('System Admin', 'admin@reserve.local', 'ChangeMe123!', 'ADMIN')
ON CONFLICT (email) DO UPDATE
SET name = EXCLUDED.name,
    password_hash = EXCLUDED.password_hash,
    role = 'ADMIN';
