package com.sentinelapi.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.sentinelapi.dto.ApiErrorResponse;

/**
 * Centralized exception handler for all REST controllers.
 *
 * <h3>How {@code @RestControllerAdvice} Works</h3>
 * <p>{@code @RestControllerAdvice} = {@code @ControllerAdvice} + {@code @ResponseBody}.
 * It intercepts exceptions thrown by any controller method and routes them to
 * the matching {@code @ExceptionHandler} method here. The return value is
 * automatically serialized to JSON (thanks to {@code @ResponseBody}).
 *
 * <h3>Exception Handler Priority</h3>
 * <p>Spring selects the most specific handler. If a {@code ResourceNotFoundException}
 * is thrown, Spring checks handlers in this order:
 * <ol>
 *   <li>{@code @ExceptionHandler(ResourceNotFoundException.class)} — exact match ✓</li>
 *   <li>{@code @ExceptionHandler(RuntimeException.class)} — parent class</li>
 *   <li>{@code @ExceptionHandler(Exception.class)} — catch-all fallback</li>
 * </ol>
 *
 * <h3>Why Return {@code ResponseEntity} Instead of Throwing?</h3>
 * <p>{@code ResponseEntity} gives full control over the HTTP response:
 * status code, headers, and body. Each handler returns the appropriate
 * HTTP status with a structured {@code ApiErrorResponse} body.
 *
 * <h3>Logging Strategy</h3>
 * <ul>
 *   <li>4xx errors (client mistakes) → logged at WARN level</li>
 *   <li>5xx errors (our mistakes) → logged at ERROR level with stack trace</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // =========================================================================
    // 400 Bad Request — Validation Errors
    // =========================================================================

    /**
     * Handles Bean Validation failures from {@code @Valid} on request bodies.
     *
     * <p>When a {@code @Valid}-annotated parameter fails validation, Spring
     * throws this exception BEFORE the controller method executes. We convert
     * each field error into a structured response.
     *
     * <p>Example trigger: POST with {@code {"name": "", "url": "not-a-url"}}
     *
     * <p>Example response:
     * <pre>
     * HTTP 400
     * {
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "Validation failed",
     *   "fieldErrors": [
     *     {"field": "name", "message": "Monitor name is required"},
     *     {"field": "url", "message": "URL must be a valid HTTP or HTTPS URL"}
     *   ],
     *   "timestamp": "2026-08-01T13:15:00Z"
     * }
     * </pre>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<ApiErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiErrorResponse.FieldError(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        log.warn("Validation failed: {} field error(s)", fieldErrors.size());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Validation failed",
                fieldErrors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles invalid enum values (e.g., httpMethod: "POST").
     *
     * <p>{@code IllegalArgumentException} is thrown by {@code HttpMethod.valueOf()}
     * in the mapper/service when an unsupported HTTP method is provided.
     *
     * <p>We provide a clear message listing the allowed values, rather than
     * exposing the raw Java exception message.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles malformed JSON request bodies.
     *
     * <p>Thrown when Jackson cannot deserialize the request body — e.g.,
     * invalid JSON syntax, wrong data types, or missing required structure.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Malformed request body. Please send valid JSON."
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles type mismatch in path variables or request parameters.
     *
     * <p>Example: {@code GET /api/v1/monitors/abc} where {@code abc} can't
     * be converted to a {@code Long} ID.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );

        log.warn("Type mismatch: {}", message);

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // =========================================================================
    // 404 Not Found
    // =========================================================================

    /**
     * Handles resource-not-found exceptions.
     *
     * <p>Thrown by service methods when {@code findById()} returns empty.
     * The exception carries the resource type and ID for a clear message.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // =========================================================================
    // 500 Internal Server Error — Catch-All
    // =========================================================================

    /**
     * Catch-all handler for unexpected exceptions.
     *
     * <p>This is the safety net. If an exception reaches here, it means we
     * didn't anticipate it — that's a bug. We log the full stack trace at
     * ERROR level and return a generic message to the client.
     *
     * <p><strong>Security:</strong> We NEVER expose the exception message or
     * stack trace to the client. Internal details like database errors,
     * NullPointerExceptions, or class names could help an attacker understand
     * our system. The client sees only "An unexpected error occurred."
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ApiErrorResponse response = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
