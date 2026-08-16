package com.sentinelapi.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a monitored API endpoint.
 *
 * <p>Each {@code Monitor} row defines:
 * <ul>
 *   <li>What URL to check</li>
 *   <li>How to check it (HTTP method, expected status, timeout)</li>
 *   <li>How often to check (check interval)</li>
 *   <li>Current state (status, consecutive failures, last checked time)</li>
 * </ul>
 *
 * <h3>Entity ↔ Table Mapping</h3>
 * <p>This class maps to the {@code monitors} table created by
 * {@code V1__initial_schema.sql}. Hibernate validates that every {@code @Column}
 * here matches a column in the database (because {@code ddl-auto=validate}).
 * If there's a mismatch, the app won't start.
 *
 * <h3>Lombok Usage</h3>
 * <p>Per the project's Lombok policy, only {@code @Getter} and {@code @Setter}
 * are used. Constructors are written explicitly for visibility and learning.
 *
 * <h3>Why no {@code @Builder}?</h3>
 * <p>Builders hide construction logic. In a learning project, seeing explicit
 * constructors and setter calls makes it clear which fields are required and
 * which have defaults. We'll use explicit construction in service and test code.
 */
@Entity
@Table(name = "monitors")
@Getter
@Setter
public class Monitor {

    // =========================================================================
    // Primary Key
    // =========================================================================

    /**
     * Auto-generated primary key.
     *
     * <p>{@code IDENTITY} strategy tells Hibernate to use the database's
     * auto-increment mechanism (PostgreSQL's {@code BIGSERIAL}).
     *
     * <p>Other strategies:
     * <ul>
     *   <li>{@code SEQUENCE} — uses a database sequence (PostgreSQL's preferred approach)</li>
     *   <li>{@code TABLE} — uses a separate table to simulate sequences (slow, avoid)</li>
     *   <li>{@code AUTO} — lets Hibernate choose (unpredictable across databases)</li>
     * </ul>
     *
     * <p>We use IDENTITY because our schema uses {@code BIGSERIAL}, which is
     * PostgreSQL's auto-increment. IDENTITY maps directly to this behavior.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================================================================
    // Configuration Fields (set by the user when creating/updating a monitor)
    // =========================================================================

    /**
     * Human-readable name for this monitor (e.g., "Payment API Health Check").
     *
     * <p>{@code nullable = false} generates a NOT NULL constraint if Hibernate
     * were managing the schema. Since Flyway manages our schema, this serves as
     * documentation and a safety net — Hibernate will throw an exception before
     * sending a NULL to the database.
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * The URL to monitor (e.g., "https://api.example.com/health").
     *
     * <p>Length 2048 accommodates very long URLs with query parameters.
     * URL validation happens at the DTO layer (Day 3), not here.
     * The entity layer is for persistence mapping, not input validation.
     */
    @Column(nullable = false, length = 2048)
    private String url;

    /**
     * HTTP method used for monitoring checks.
     *
     * <p>{@code @Enumerated(EnumType.STRING)} stores the enum as its name
     * (e.g., "GET", "HEAD") rather than its ordinal (0, 1).
     *
     * <p><strong>Why STRING over ORDINAL?</strong> ORDINAL stores UP=0, DOWN=1.
     * If you reorder the enum or insert a new value, every row in the database
     * silently gets the wrong value. STRING is immune to this because it stores
     * the actual name. Always use STRING.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    private HttpMethod httpMethod = HttpMethod.GET;

    /**
     * The HTTP status code that indicates a healthy endpoint.
     * Default: 200. The monitoring engine compares the actual response status
     * against this value to determine UP vs DOWN.
     */
    @Column(name = "expected_status", nullable = false)
    private Integer expectedStatus = 200;

    /**
     * How often to check this endpoint, in seconds.
     * Default: 300 (5 minutes). Minimum: 300 in V1.
     *
     * <p>Stored as seconds (not minutes) to allow future sub-minute intervals
     * without a database migration.
     */
    @Column(name = "check_interval", nullable = false)
    private Integer checkInterval = 300;

    /**
     * Request timeout in milliseconds.
     * Default: 5000 (5 seconds). Maximum: 10000 in V1.
     *
     * <p>If the monitored endpoint doesn't respond within this time,
     * the check is marked as failed with error type TIMEOUT.
     */
    @Column(nullable = false)
    private Integer timeout = 5000;

    /**
     * Whether this monitor is actively being checked by the scheduler.
     * When set to false, the scheduler skips this monitor.
     */
    @Column(nullable = false)
    private Boolean active = true;

    // =========================================================================
    // Runtime State Fields (managed by the monitoring engine, not user input)
    // =========================================================================

    /**
     * Current operational status of this monitor.
     *
     * <p>This is runtime state managed by the monitoring engine:
     * <ul>
     *   <li>New monitors start as UNKNOWN</li>
     *   <li>Moves to UP on first successful check</li>
     *   <li>Moves to DOWN when consecutive failures reach the threshold</li>
     *   <li>Moves to PAUSED when active is set to false</li>
     * </ul>
     *
     * <p>Users cannot set this directly via the API — it is computed
     * by the monitoring engine based on check results.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MonitorStatus status = MonitorStatus.UNKNOWN;

    /**
     * Number of consecutive failed checks.
     * Resets to 0 on a successful check.
     *
     * <p>When this reaches {@link #failureThreshold}, the monitor's status
     * changes to DOWN. Tracking this on the entity (rather than computing it
     * from results) is O(1) vs. scanning the last N results every check — a
     * critical performance optimization for production systems.
     */
    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    /**
     * How many consecutive failures are needed before marking the monitor as DOWN.
     * Default: 3.
     *
     * <p>This prevents transient network blips from triggering false alarms.
     * A single timeout shouldn't mark a healthy service as DOWN — but three
     * in a row is a strong signal of a real outage.
     */
    @Column(name = "failure_threshold", nullable = false)
    private Integer failureThreshold = 3;

    /**
     * When this monitor was last checked by the monitoring engine.
     * NULL means it has never been checked.
     *
     * <p>The scheduler uses this to determine which monitors are "due" for
     * a check: {@code WHERE last_checked_at IS NULL OR last_checked_at < (now - check_interval)}.
     *
     * <p>{@code Instant} represents a point in time in UTC. It is the correct
     * Java type for timestamps that represent "when something happened."
     * Never use {@code LocalDateTime} for this — it has no timezone and would
     * silently produce wrong results when the server timezone changes.
     *
     * <p>{@code columnDefinition = "TIMESTAMPTZ"} tells Hibernate that the
     * database column is PostgreSQL's TIMESTAMP WITH TIME ZONE.
     */
    @Column(name = "last_checked_at", columnDefinition = "TIMESTAMPTZ")
    private Instant lastCheckedAt;

    // =========================================================================
    // Audit Fields
    // =========================================================================

    /**
     * When this monitor was created. Set once, never updated.
     *
     * <p>In a production app, you'd use {@code @CreationTimestamp} or
     * Spring Data's {@code @CreatedDate} with auditing enabled. We set it
     * explicitly for clarity — the database DEFAULT NOW() handles the initial
     * value, and we never overwrite it.
     */
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant createdAt;

    /**
     * When this monitor was last updated.
     *
     * <p>Updated by the service layer on every PATCH operation.
     * The database sets the initial value via DEFAULT NOW().
     */
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private Instant updatedAt;

    // =========================================================================
    // Constructors
    // =========================================================================

    /**
     * No-args constructor required by JPA.
     *
     * <p>JPA (Hibernate) needs a no-arg constructor to create entity instances
     * via reflection when loading data from the database. It can be
     * {@code protected} — JPA doesn't require it to be public. We make it
     * protected to signal that application code should not use it directly.
     */
    protected Monitor() {
        // Required by JPA
    }

    /**
     * Constructor for creating a new monitor with required fields.
     * Optional fields use their defaults defined above.
     *
     * @param name the human-readable name for this monitor
     * @param url  the URL to monitor
     */
    public Monitor(String name, String url) {
        this.name = name;
        this.url = url;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // Object Methods
    // =========================================================================

    /**
     * String representation for logging and debugging.
     * Intentionally written explicitly instead of using Lombok's @ToString.
     */
    @Override
    public String toString() {
        return "Monitor{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", httpMethod=" + httpMethod +
                ", status=" + status +
                ", active=" + active +
                '}';
    }
}
