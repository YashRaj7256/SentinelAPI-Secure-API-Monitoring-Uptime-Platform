package com.sentinelapi.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for monitor data returned by the API.
 *
 * <h3>Why a Separate Response DTO?</h3>
 * <p>The response includes computed fields ({@code status}, {@code consecutiveFailures},
 * {@code lastCheckedAt}) that are not part of the create/patch requests. It also
 * provides a stable API contract — internal entity changes don't affect the response.
 *
 * <h3>Field Types</h3>
 * <p>All fields use the same types as the entity. {@code Instant} timestamps are
 * serialized by Jackson as ISO-8601 strings (e.g., {@code "2026-08-01T13:15:00Z"})
 * which is the standard format for REST APIs.
 */
@Getter
@Setter
public class MonitorResponse {

    private Long id;
    private String name;
    private String url;
    private String httpMethod;
    private Integer expectedStatus;
    private Integer checkInterval;
    private Integer timeout;
    private Boolean active;
    private String status;
    private Integer failureThreshold;
    private Integer consecutiveFailures;
    private Instant lastCheckedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
