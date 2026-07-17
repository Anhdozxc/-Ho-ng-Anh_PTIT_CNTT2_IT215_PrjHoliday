-- Run once only when upgrading an existing Holiday Planner MySQL database.
-- Fresh local/demo databases can continue to use Hibernate ddl-auto=update.
ALTER TABLE users
    ADD COLUMN avatar_url VARCHAR(700) NULL,
    ADD COLUMN avatar_public_id VARCHAR(255) NULL;

ALTER TABLE destinations
    ADD COLUMN image_public_id VARCHAR(255) NULL;
