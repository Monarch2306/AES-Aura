-- Idempotent patches for databases created from an older schema.sql.
-- Hibernate ddl-auto=update will not add NOT NULL columns to tables that
-- already contain rows, which previously broke registration/login.

ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE users ALTER COLUMN status SET DEFAULT 'ACTIVE';
ALTER TABLE users ALTER COLUMN status SET NOT NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT now();
UPDATE users SET updated_at = COALESCE(created_at, now()) WHERE updated_at IS NULL;
ALTER TABLE users ALTER COLUMN updated_at SET DEFAULT now();
ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;
