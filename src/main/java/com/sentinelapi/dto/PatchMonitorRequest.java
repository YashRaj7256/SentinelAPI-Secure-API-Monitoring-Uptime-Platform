package com.sentinelapi.dto;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for partially updating a monitor via {@code PATCH /api/v1/monitors/{id}}.
 *
 * <h3>PATCH Semantics</h3>
 * <p>All fields are nullable. Only the fields included in the request body are
 * updated — omitted fields remain unchanged on the monitor entity.
 *
 * <p>Example: To change only the check interval:
 * <pre>
 * PATCH /api/v1/monitors/1
 * { "checkInterval": 600 }
 * </pre>
 * All other fields (name, url, timeout, etc.) remain unchanged.
 *
 * <h3>How Null Handling Works</h3>
 * <p>When Jackson deserializes the request body:
 * <ul>
 *   <li>Field present in JSON → field is set to the provided value</li>
 *   <li>Field absent from JSON → field remains {@code null} (its default)</li>
 * </ul>
 * <p>The service layer checks each field: if non-null, update it on the entity.
 *
 * <h3>Why All Fields Are Nullable</h3>
 * <p>If we used primitives ({@code int} instead of {@code Integer}), Java would
 * default them to 0, and we couldn't tell the difference between "the client
 * didn't send this field" and "the client sent 0". Using {@code Integer}
 * (the wrapper type), the default is {@code null} — which clearly means "not provided".
 *
 * <h3>Validation</h3>
 * <p>Validation annotations apply <strong>only when the field is provided</strong>
 * (non-null). For example, {@code @Min(300)} on {@code checkInterval} means:
 * "if a check interval is provided, it must be at least 300." If the field
 * is null (not provided), validation is skipped for that field.
 *
 * <h3>No {@code @NotBlank} / {@code @NotNull}</h3>
 * <p>Unlike {@code CreateMonitorRequest}, there are no required fields here.
 * A PATCH request with an empty body {@code {}} is valid — it simply changes nothing.
 *
 * <h3>Fields NOT Updatable via PATCH</h3>
 * <p>These fields are intentionally excluded:
 * <ul>
 *   <li>{@code status} — managed by the monitoring engine, not user input</li>
 *   <li>{@code consecutiveFailures} — runtime state, reset by the monitoring engine</li>
 *   <li>{@code lastCheckedAt} — set by the monitoring engine</li>
 *   <li>{@code createdAt} — immutable audit field</li>
 * </ul>
 */
@Getter
@Setter
public class PatchMonitorRequest {

    /**
     * Updated monitor name.
     *
     * <p>No {@code @NotBlank} — the field is optional for PATCH. But if provided,
     * {@code @Size} ensures it's within bounds. An empty/blank name will be
     * rejected by the service layer explicitly (since we don't want blank
     * names but can't use @NotBlank on an optional field).
     */
    @Size(min = 1, max = 255, message = "Monitor name must be between 1 and 255 characters")
    private String name;

    /**
     * Updated URL to monitor.
     *
     * <p>If provided, must be a valid URL within 2048 chars.
     */
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @URL(message = "URL must be a valid HTTP or HTTPS URL")
    private String url;

    /**
     * Updated HTTP method.
     *
     * <p>If provided, must be "GET" or "HEAD". Validated in the service layer
     * via {@code HttpMethod.valueOf()} — not via Bean Validation — so we can
     * provide a clear, specific error message.
     */
    private String httpMethod;

    /**
     * Updated expected HTTP status code.
     */
    @Min(value = 100, message = "Expected status must be a valid HTTP status code (100-599)")
    @Max(value = 599, message = "Expected status must be a valid HTTP status code (100-599)")
    private Integer expectedStatus;

    /**
     * Updated check interval in seconds.
     */
    @Min(value = 300, message = "Check interval must be at least 300 seconds (5 minutes)")
    @Max(value = 86400, message = "Check interval must not exceed 86400 seconds (24 hours)")
    private Integer checkInterval;

    /**
     * Updated timeout in milliseconds.
     */
    @Min(value = 1000, message = "Timeout must be at least 1000 milliseconds (1 second)")
    @Max(value = 10000, message = "Timeout must not exceed 10000 milliseconds (10 seconds)")
    private Integer timeout;

    /**
     * Updated active flag.
     *
     * <p>Setting this to {@code false} pauses the monitor (the scheduler
     * will skip it). Setting it to {@code true} reactivates it.
     */
    private Boolean active;

    /**
     * Updated failure threshold.
     */
    @Min(value = 1, message = "Failure threshold must be at least 1")
    @Max(value = 10, message = "Failure threshold must not exceed 10")
    private Integer failureThreshold;
}
