ALTER TABLE reserves
    ADD COLUMN manager_user_id BIGINT;

UPDATE reserves r
SET manager_user_id = assignments.user_id
FROM (
    SELECT reserve_id, MIN(user_id) AS user_id
    FROM admin_reserves
    GROUP BY reserve_id
) assignments
WHERE r.id = assignments.reserve_id;

ALTER TABLE reserves
    ADD CONSTRAINT reserves_manager_user_fk
        FOREIGN KEY (manager_user_id) REFERENCES users(id);

CREATE INDEX idx_reserves_manager_user_id ON reserves(manager_user_id);

DROP TABLE IF EXISTS admin_reserves;
