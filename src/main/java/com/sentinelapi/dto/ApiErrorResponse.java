package com.sentinelapi.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

/**
 * Standardized error response for all API errors.
 *
 * <h3>Why a Custom Error Response?</h3>
 * <p>Spring Boot's default error response includes fields like {@code trace}
 * (full stack trace) and {@code path} in an inconsistent format. A custom
 * error response provides:
 * <ul>
 *   <li>Consistent structure across all error types</li>
 *   <li>No stack trace leakage (security risk in production)</li>
 *   <li>Structured validation errors (field-level detail)</li>
 *   <li>Machine-readable error codes for client-side handling</li>
 * </ul>
 *
 * <h3>Example Responses</h3>
 * <p>Validation error (400):
 * <pre>
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "Validation failed",
 *   "fieldErrors": [
 *     { "field": "name", "message": "Monitor name is required" },
 *     { "field": "url", "message": "URL must be a valid HTTP or HTTPS URL" }
 *   ],
 *   "timestamp": "2026-08-01T13:15:00Z"
 * }
 * </pre>
 *
 * <p>Not Found error (404):
 * <pre>
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Monitor not found with id: 999",
 *   "timestamp": "2026-08-01T13:15:00Z"
 * }
 * </pre>
 *
 * <h3>{@code @JsonInclude(NON_NULL)}</h3>
 * <p>This Jackson annotation omits null fields from the JSON output.
 * For a 404 error, {@code fieldErrors} is null and won't appear in the
 * response — keeping it clean.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    /** HTTP status code (e.g., 400, 404, 500). */
    private final int status;

    /** HTTP status reason phrase (e.g., "Bad Request", "Not Found"). */
    private final String error;

    /** Human-readable error message. */
    private final String message;

    /** Field-level validation errors. Null for non-validation errors. */
    private final List<FieldError> fieldErrors;

    /** When the error occurred. */
    private final Instant timestamp;

    /**
     * Constructor for errors without field-level details (404, 500, etc.).
     */
    public ApiErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = null;
        this.timestamp = Instant.now();
    }

    /**
     * Constructor for validation errors with field-level details (400).
     */
    public ApiErrorResponse(int status, String error, String message, List<FieldError> fieldErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.timestamp = Instant.now();
    }

    /**
     * Represents a single field validation error.
     *
     * <p>A Java {@code record} is a concise class for immutable data.
     * The compiler auto-generates constructor, getters, equals, hashCode, toString.
     * Records are ideal for simple data carriers like this.
     *
     * @param field   the field name that failed validation
     * @param message the validation error message
     */
    public record FieldError(String field, String message) {}
}
