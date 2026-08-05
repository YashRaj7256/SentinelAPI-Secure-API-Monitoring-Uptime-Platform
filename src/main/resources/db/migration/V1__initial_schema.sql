-- =============================================================================
-- Flyway Migration: V1__initial_schema.sql
-- =============================================================================
-- Creates the initial SentinelAPI database schema.
--
-- Naming convention: V{version}__{description}.sql
--   - 'V1' means this is version 1 (Flyway runs migrations in version order)
--   - Double underscore '__' separates version from description
--   - Flyway tracks which migrations have already been applied in a
--     'flyway_schema_history' table so it never runs the same migration twice
--
-- IMPORTANT: Once this migration has been run, it must NEVER be modified.
-- Future schema changes must go in new migration files (V2__, V3__, etc.).
-- This is how database schema evolution works in production systems.
-- =============================================================================

-- ---- MONITORS TABLE ----
-- Stores API endpoint configurations that SentinelAPI monitors.
-- Each row represents one monitored endpoint.
--
-- V1 is single-user: no users table, no user_id column.
-- Phase 2 will add users and ownership via a separate Flyway migration,
-- demonstrating real database schema evolution.
CREATE TABLE monitors (
    -- BIGSERIAL: auto-incrementing 64-bit integer. PostgreSQL-specific.
    -- Equivalent to BIGINT with an auto-increment sequence.
    id                  BIGSERIAL       PRIMARY KEY,

    -- Human-readable name for this monitor (e.g., "Payment Service Health")
    name                VARCHAR(255)    NOT NULL,

    -- The URL to monitor (e.g., "https://example.com/api/health")
    -- 2048 chars accommodates very long URLs with query parameters
    url                 VARCHAR(2048)   NOT NULL,

    -- HTTP method: GET or HEAD only in V1 (validated at application layer)
    http_method         VARCHAR(10)     NOT NULL DEFAULT 'GET',

    -- The HTTP status code we expect from a healthy endpoint
    expected_status     INTEGER         NOT NULL DEFAULT 200,

    -- How often to check, in seconds. Default: 300 (5 minutes)
    check_interval      INTEGER         NOT NULL DEFAULT 300,

    -- Request timeout in milliseconds. Default: 5000 (5 seconds)
    timeout             INTEGER         NOT NULL DEFAULT 5000,

    -- Whether this monitor is actively being checked
    active              BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Current monitor status: UP, DOWN, UNKNOWN, PAUSED
    -- UNKNOWN = never checked yet. PAUSED = manually deactivated.
    status              VARCHAR(20)     NOT NULL DEFAULT 'UNKNOWN',

    -- Number of consecutive failed checks (resets to 0 on success)
    consecutive_failures INTEGER       NOT NULL DEFAULT 0,

    -- How many consecutive failures before marking DOWN (default: 3)
    failure_threshold   INTEGER         NOT NULL DEFAULT 3,

    -- When this monitor was last checked (NULL = never checked)
    -- TIMESTAMPTZ: timestamp WITH time zone. PostgreSQL stores it in UTC internally.
    -- This is the correct type for "when did this happen" timestamps.
    -- NEVER use TIMESTAMP (without TZ) — it loses timezone information.
    last_checked_at     TIMESTAMPTZ,

    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);


-- ---- MONITOR RESULTS TABLE ----
-- Stores the result of every individual monitoring check.
-- This table grows continuously and may become very large in production.
-- Scalability considerations (partitioning, retention, archiving) will be
-- addressed in later phases.
CREATE TABLE monitor_results (
    id                  BIGSERIAL       PRIMARY KEY,

    -- Foreign key to the monitors table.
    -- ON DELETE CASCADE: when a monitor is deleted, all its results are deleted too.
    -- In production, you might archive results before deleting — but for V1, cascade is appropriate.
    monitor_id          BIGINT          NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,

    -- Result of this check: 'UP' or 'DOWN'
    status              VARCHAR(10)     NOT NULL,

    -- HTTP status code received (NULL if the request failed entirely, e.g., timeout)
    http_status         INTEGER,

    -- Response latency in milliseconds (NULL if the request failed entirely)
    response_time_ms    INTEGER,

    -- Error classification enum: TIMEOUT, CONNECTION_REFUSED, DNS_FAILURE, etc.
    -- NULL when the check succeeded (status = UP)
    error_type          VARCHAR(50),

    -- Human-readable error detail for debugging
    -- NULL when the check succeeded
    error_message       VARCHAR(1000),

    -- When this check was performed
    checked_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);


-- ---- INDEXES ----

-- Index for querying a monitor's check history, most recent first.
-- Used by: GET /api/v1/monitors/{id}/history
-- This is a composite index: (monitor_id, checked_at DESC)
-- PostgreSQL can use this for both filtering by monitor_id AND sorting by checked_at.
CREATE INDEX idx_monitor_results_monitor_checked
    ON monitor_results(monitor_id, checked_at DESC);

-- Index for the scheduler: quickly find active monitors that are due for a check.
-- The scheduler queries: WHERE active = true AND (last_checked_at IS NULL OR last_checked_at < ...)
CREATE INDEX idx_monitors_active_last_checked
    ON monitors(active, last_checked_at);
