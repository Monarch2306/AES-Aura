-- Portable compatibility patches. Safe to re-run.
-- PostgreSQL and H2 2.x both support ADD COLUMN IF NOT EXISTS.

ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
