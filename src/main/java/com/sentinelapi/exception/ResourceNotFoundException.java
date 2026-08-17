package com.sentinelapi.exception;

/**
 * Thrown when a requested resource does not exist in the database.
 *
 * <h3>Why a Custom Exception?</h3>
 * <p>Spring Boot's default behavior when an entity is not found depends on
 * how you look it up:
 * <ul>
 *   <li>{@code findById()} returns {@code Optional.empty()} — you'd need to
 *       check and throw manually every time</li>
 *   <li>{@code getReferenceById()} throws {@code EntityNotFoundException} — but
 *       that's a JPA exception that doesn't carry context (which entity? which ID?)</li>
 * </ul>
 *
 * <p>This custom exception:
 * <ul>
 *   <li>Carries the resource type and identifier for clear error messages</li>
 *   <li>Is caught by the global exception handler (Day 6) and mapped to HTTP 404</li>
 *   <li>Can be thrown from any service, not just monitor-related code</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * Monitor monitor = monitorRepository.findById(id)
 *     .orElseThrow(() -> new ResourceNotFoundException("Monitor", id));
 * // If we get here, monitor is guaranteed non-null
 * </pre>
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final Object resourceId;

    /**
     * Creates a new ResourceNotFoundException.
     *
     * @param resourceName the type of resource (e.g., "Monitor", "User")
     * @param resourceId   the identifier that was not found
     */
    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(String.format("%s not found with id: %s", resourceName, resourceId));
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
