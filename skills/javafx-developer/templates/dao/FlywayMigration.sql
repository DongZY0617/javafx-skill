-- =============================================================================
-- Flyway versioned migration template
-- =============================================================================
-- File naming convention (see references/database-integration.md section 5.3):
--   Versioned:   V{version}__{Description}.sql   (e.g. V1__Create_users_table.sql)
--   Repeatable: R__{Description}.sql             (e.g. R__Refresh_user_views.sql)
--
-- Rules:
--   1. NEVER edit an already-applied migration — changing its checksum fails
--      `validate-on-migrate`. Add a new V{n} migration instead.
--   2. Keep each V-script idempotent-friendly: running all V-scripts in order on
--      a fresh DB must produce the current schema.
--   3. Use repeatable R-scripts for views, stored procedures, and triggers;
--      they re-run whenever their checksum changes (after every V* change).
--   4. Prefer explicit column types and constraints (NOT NULL, defaults,
--      CHECK) over implicit ones.
--   5. Foreign keys and indexes must reference real columns created in an
--      earlier or the same migration.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- V1__Create_users_table.sql
-- Creates the users table backing the User JPA entity (see templates/dao/Entity.java)
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id     BIGINT       NOT NULL AUTO_INCREMENT,
    name   VARCHAR(100) NOT NULL,
    email  VARCHAR(200) NOT NULL,
    active BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- Index for email lookups (findByEmail, login by email)
CREATE INDEX idx_users_email ON users (email);

-- Index for name search (the UserMapper.findPage / UserRepository.search queries)
CREATE INDEX idx_users_name ON users (name);

-- -----------------------------------------------------------------------------
-- Seed default data (optional; prefer a separate V{n} migration for data)
-- -----------------------------------------------------------------------------
INSERT INTO users (name, email, active) VALUES
    ('Administrator', 'admin@example.com', TRUE),
    ('Demo User',     'demo@example.com',  TRUE);

-- -----------------------------------------------------------------------------
-- Repeatable migration example (R__Create_user_summary_view.sql)
-- Re-runs whenever its checksum changes; place in db/migration/ alongside V*.
-- -----------------------------------------------------------------------------
-- CREATE OR REPLACE VIEW v_user_summary AS
--     SELECT id, name, email, active
--     FROM users
--     WHERE active = TRUE;
