package com.sentinelapi.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating a new monitor via {@code POST /api/v1/monitors}.
 *
 * <h3>Why a DTO Instead of the Monitor Entity?</h3>
 * <ol>
 *   <li><strong>Over-posting protection</strong> — clients can't set runtime fields
 *       like {@code status} or {@code consecutiveFailures}</li>
 *   <li><strong>Different validation</strong> — {@code name} and {@code url} are required
 *       here but optional in {@code PatchMonitorRequest}</li>
 *   <li><strong>API stability</strong> — entity schema changes don't break the API contract</li>
 *   <li><strong>No information leakage</strong> — internal fields never appear in the request</li>
 * </ol>
 *
 * <h3>Bean Validation</h3>
 * <p>Annotations like {@code @NotBlank}, {@code @Size}, {@code @Min}, {@code @Max}
 * are from the <em>Jakarta Bean Validation</em> specification (formerly javax.validation).
 * Spring automatically validates DTOs annotated with {@code @Valid} in controller
 * method parameters. If validation fails, Spring returns a 400 Bad Request
 * <strong>before your controller code even executes</strong>.
 *
 * <h3>Default Values</h3>
 * <p>Optional fields have defaults assigned here. The service layer will use these
 * defaults when the client omits them from the request body. This keeps default
 * logic in one place (the DTO) rather than scattered across service and entity.
 */
@Getter
@Setter
public class CreateMonitorRequest {

    /**
     * Human-readable name for the monitor.
     *
     * <p>{@code @NotBlank} rejects null, empty string "", and whitespace-only "   ".
     * It is stricter than {@code @NotNull} (which allows empty strings) and
     * {@code @NotEmpty} (which allows whitespace-only strings).
     *
     * <p>{@code @Size} enforces length constraints. {@code max = 255} matches the
     * VARCHAR(255) column in the database — validation catches oversized input
     * before it hits the database constraint, giving a better error message.
     */
    @NotBlank(message = "Monitor name is required")
    @Size(max = 255, message = "Monitor name must not exceed 255 characters")
    private String name;

    /**
     * The URL to monitor.
     *
     * <p>{@code @NotBlank} — required field.
     *
     * <p>{@code @URL} is from Hibernate Validator (not the JPA spec). It validates
     * that the string is a well-formed URL. We don't use {@code @Pattern} with a
     * regex because URL validation is surprisingly complex (internationalized
     * domain names, IPv6 addresses, encoded characters, etc.) and Hibernate
     * Validator handles all edge cases correctly.
     *
     * <p>Note: {@code @URL} validates <em>format</em>, not <em>reachability</em>.
     * The monitoring engine determines whether the URL is reachable.
     *
     * <p><strong>Security note:</strong> URL format validation does NOT prevent SSRF.
     * A valid URL can still point to internal services (e.g., http://169.254.169.254/).
     * SSRF protection is implemented separately in Phase 3.
     */
    @NotBlank(message = "Monitor URL is required")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @URL(message = "URL must be a valid HTTP or HTTPS URL")
    private String url;

    /**
     * HTTP method for monitoring checks.
     *
     * <p>Optional — defaults to "GET". Accepts: "GET", "HEAD".
     *
     * <p>This field is a {@code String}, not an {@code HttpMethod} enum.
     * The service layer converts it to the enum using {@code HttpMethod.valueOf()},
     * which throws an exception if the value is invalid. This is handled by
     * the global exception handler (Day 6).
     *
     * <p><strong>Why String instead of HttpMethod?</strong>
     * If we used the enum type directly, Jackson would throw a generic
     * deserialization error for invalid values like "POST". By using a String,
     * we control the error message and can provide a clear explanation:
     * "Unsupported HTTP method: POST. Allowed: GET, HEAD."
     */
    private String httpMethod = "GET";

    /**
     * Expected HTTP status code from a healthy endpoint.
     *
     * <p>Optional — defaults to 200. Range: 100-599.
     *
     * <p>{@code @Min} and {@code @Max} work on numeric types. They validate
     * that the value is within the HTTP status code range. Note that this
     * accepts any status code (including 4xx/5xx) because some health endpoints
     * intentionally return non-200 codes.
     */
    @Min(value = 100, message = "Expected status must be a valid HTTP status code (100-599)")
    @Max(value = 599, message = "Expected status must be a valid HTTP status code (100-599)")
    private Integer expectedStatus = 200;

    /**
     * How often to check this endpoint, in seconds.
     *
     * <p>Optional — defaults to 300 (5 minutes).
     * Min: 300 (5 min) — prevents abuse by checking too frequently.
     * Max: 86400 (24 hours) — checks less frequent than daily aren't useful monitoring.
     *
     * <p><strong>Why minimum 300?</strong> At 1-second intervals with 1000 monitors,
     * that's 1000 HTTP requests per second — enough to look like a DDoS attack.
     * In production, shorter intervals would require rate limiting and worker pools.
     */
    @Min(value = 300, message = "Check interval must be at least 300 seconds (5 minutes)")
    @Max(value = 86400, message = "Check interval must not exceed 86400 seconds (24 hours)")
    private Integer checkInterval = 300;

    /**
     * Request timeout in milliseconds.
     *
     * <p>Optional — defaults to 5000 (5 seconds).
     * Min: 1000 (1 second) — shorter timeouts cause false positives.
     * Max: 10000 (10 seconds) — longer timeouts block the scheduler thread.
     */
    @Min(value = 1000, message = "Timeout must be at least 1000 milliseconds (1 second)")
    @Max(value = 10000, message = "Timeout must not exceed 10000 milliseconds (10 seconds)")
    private Integer timeout = 5000;

    /**
     * How many consecutive failures before marking the monitor as DOWN.
     *
     * <p>Optional — defaults to 3.
     * Min: 1 — at least one failure required.
     * Max: 10 — more than 10 consecutive checks before alerting is too slow.
     *
     * <p><strong>Why default 3?</strong> A single failed check could be a transient
     * network blip. Three consecutive failures strongly indicate a real outage.
     * This prevents alert fatigue from false positives.
     */
    @Min(value = 1, message = "Failure threshold must be at least 1")
    @Max(value = 10, message = "Failure threshold must not exceed 10")
    private Integer failureThreshold = 3;
}
