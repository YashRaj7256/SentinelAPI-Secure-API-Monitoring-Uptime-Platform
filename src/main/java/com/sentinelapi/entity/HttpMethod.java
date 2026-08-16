package com.sentinelapi.entity;

/**
 * HTTP methods supported for endpoint monitoring.
 *
 * <p>This enum maps to the {@code http_method} VARCHAR column in the {@code monitors} table.
 * It is stored as a STRING in the database via {@code @Enumerated(EnumType.STRING)}.
 *
 * <h3>V1: GET and HEAD only</h3>
 * <p>SentinelAPI is an <em>uptime/availability</em> monitor. Automatically executing
 * state-changing HTTP methods ({@code POST}, {@code PUT}, {@code DELETE}) against
 * external services at recurring intervals could cause:
 * <ul>
 *   <li>Duplicate transactions (e.g., creating orders every 5 minutes)</li>
 *   <li>Data mutations (e.g., deleting resources)</li>
 *   <li>Unexpected resource creation</li>
 * </ul>
 *
 * <p>{@code GET} and {@code HEAD} are safe, idempotent methods designed for retrieval.
 * They are the standard methods used by uptime monitoring tools.
 *
 * <h3>Future Extensibility</h3>
 * <p>To add a new method (e.g., {@code OPTIONS}) in a future version:
 * <ol>
 *   <li>Add the value to this enum</li>
 *   <li>Update DTO validation if applicable</li>
 *   <li>The monitoring engine, database, and controllers require no changes</li>
 * </ol>
 *
 * <h3>Why a custom enum instead of Spring's {@code HttpMethod}?</h3>
 * <p>Spring's {@code org.springframework.http.HttpMethod} includes all HTTP methods
 * including dangerous ones like POST and DELETE. Using our own enum ensures that
 * the <strong>type system itself</strong> restricts which methods can be used for
 * monitoring — it's impossible to accidentally pass {@code POST} to the monitoring engine.
 */
public enum HttpMethod {

    /** HTTP GET — retrieves a resource. Safe and idempotent. */
    GET,

    /**
     * HTTP HEAD — identical to GET but returns only headers, no response body.
     * Useful for monitoring when you only care about status code and latency,
     * not the response content. Uses less bandwidth than GET.
     */
    HEAD
}
