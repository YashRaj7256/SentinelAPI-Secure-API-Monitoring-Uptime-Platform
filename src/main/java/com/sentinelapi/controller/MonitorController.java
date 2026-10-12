package com.sentinelapi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sentinelapi.dto.CreateMonitorRequest;
import com.sentinelapi.dto.MonitorResponse;
import com.sentinelapi.dto.PageResponse;
import com.sentinelapi.dto.PatchMonitorRequest;
import com.sentinelapi.service.MonitorService;

import jakarta.validation.Valid;

/**
 * REST controller for Monitor CRUD operations.
 *
 * <h3>Controller Responsibilities</h3>
 * <p>The controller is a <strong>thin layer</strong>. It handles:
 * <ul>
 *   <li>HTTP concerns — status codes, request/response mapping</li>
 *   <li>Triggering validation ({@code @Valid})</li>
 *   <li>Delegating to the service layer for all business logic</li>
 * </ul>
 *
 * <p>Business logic does NOT belong here. If you find yourself writing
 * if-else logic, database queries, or data transformations in a controller,
 * it should be moved to the service layer.
 *
 * <h3>Annotations Explained</h3>
 * <ul>
 *   <li>{@code @RestController} = {@code @Controller} + {@code @ResponseBody}.
 *       Every method's return value is serialized to JSON automatically.</li>
 *   <li>{@code @RequestMapping("/api/v1/monitors")} — base path for all endpoints
 *       in this controller. Individual methods add to this path.</li>
 * </ul>
 *
 * <h3>API Versioning</h3>
 * <p>The {@code /api/v1/} prefix versions the API from day one. This costs
 * nothing now but prevents painful URL changes if we ever need a V2 with
 * breaking changes. Clients can migrate at their own pace.
 */
@RestController
@RequestMapping("/api/v1/monitors")
public class MonitorController {

    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    private final MonitorService monitorService;

    /**
     * Constructor injection — Spring injects the MonitorService automatically.
     */
    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    // =========================================================================
    // POST /api/v1/monitors — Create a Monitor
    // =========================================================================

    /**
     * Creates a new monitor.
     *
     * <p>{@code @PostMapping} maps HTTP POST requests to this method.
     *
     * <p>{@code @Valid} triggers Bean Validation on the request body. If
     * validation fails, Spring throws {@code MethodArgumentNotValidException}
     * which our {@code GlobalExceptionHandler} catches and converts to a
     * 400 response — this method never executes.
     *
     * <p>{@code @RequestBody} tells Spring to deserialize the HTTP request
     * body (JSON) into a {@code CreateMonitorRequest} object using Jackson.
     *
     * <p>Returns HTTP 201 Created (not 200 OK) because a new resource was
     * created. This is the correct REST semantic for POST that creates.
     *
     * @param request the validated create request
     * @return the created monitor with HTTP 201
     */
    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(
            @Valid @RequestBody CreateMonitorRequest request) {

        log.debug("POST /api/v1/monitors — name='{}'", request.getName());

        MonitorResponse response = monitorService.createMonitor(request);

        // ResponseEntity.status(201).body(response) — returns 201 Created
        // with the full monitor response as the body.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // GET /api/v1/monitors — List All Monitors (Paginated)
    // =========================================================================

    /**
     * Lists all monitors with pagination.
     *
     * <p>{@code @RequestParam} extracts query parameters from the URL.
     * {@code defaultValue} provides fallbacks when the client omits them.
     *
     * <p>Example: {@code GET /api/v1/monitors?page=0&size=20}
     *
     * <p>Returns HTTP 200 OK with paginated content.
     *
     * @param page page number (0-based, default 0)
     * @param size page size (default 20, capped at 100 by the service)
     * @return paginated list of monitors
     */
    @GetMapping
    public ResponseEntity<PageResponse<MonitorResponse>> getAllMonitors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("GET /api/v1/monitors — page={}, size={}", page, size);

        PageResponse<MonitorResponse> response = monitorService.getAllMonitors(page, size);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/v1/monitors/{id} — Get Monitor by ID
    // =========================================================================

    /**
     * Retrieves a single monitor by its ID.
     *
     * <p>{@code @PathVariable} extracts the {@code {id}} segment from the URL.
     * Spring automatically converts the string to a {@code Long}.
     *
     * <p>If the ID doesn't exist, the service throws
     * {@code ResourceNotFoundException} which the exception handler
     * converts to HTTP 404.
     *
     * <p>If the ID isn't a valid number (e.g., "/monitors/abc"),
     * {@code MethodArgumentTypeMismatchException} is thrown, which our
     * exception handler converts to HTTP 400.
     *
     * @param id the monitor ID from the URL path
     * @return the monitor with HTTP 200
     */
    @GetMapping("/{id}")
    public ResponseEntity<MonitorResponse> getMonitorById(@PathVariable Long id) {

        log.debug("GET /api/v1/monitors/{}", id);

        MonitorResponse response = monitorService.getMonitorById(id);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PATCH /api/v1/monitors/{id} — Partial Update
    // =========================================================================

    /**
     * Partially updates a monitor — only provided fields are changed.
     *
     * <p>{@code @PatchMapping} maps HTTP PATCH requests.
     *
     * <p>PATCH semantics: the client sends only the fields they want to
     * change. Omitted fields remain unchanged. This is different from PUT,
     * which would require sending the entire resource.
     *
     * <p>Example: {@code PATCH /api/v1/monitors/1} with body
     * {@code {"checkInterval": 600}} — only updates checkInterval.
     *
     * @param id      the monitor ID from the URL path
     * @param request the partial update request (all fields nullable)
     * @return the fully updated monitor with HTTP 200
     */
    @PatchMapping("/{id}")
    public ResponseEntity<MonitorResponse> patchMonitor(
            @PathVariable Long id,
            @Valid @RequestBody PatchMonitorRequest request) {

        log.debug("PATCH /api/v1/monitors/{}", id);

        MonitorResponse response = monitorService.patchMonitor(id, request);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // DELETE /api/v1/monitors/{id} — Delete Monitor
    // =========================================================================

    /**
     * Deletes a monitor and all its associated results.
     *
     * <p>{@code @DeleteMapping} maps HTTP DELETE requests.
     *
     * <p>Returns HTTP 204 No Content — the standard response for successful
     * DELETE operations. The body is empty because the resource no longer exists.
     *
     * <p>{@code ResponseEntity.noContent().build()} creates a 204 response
     * with no body. {@code Void} as the type parameter indicates no response body.
     *
     * <p>If the monitor doesn't exist, returns 404 (the service checks existence).
     *
     * @param id the monitor ID from the URL path
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(@PathVariable Long id) {

        log.debug("DELETE /api/v1/monitors/{}", id);

        monitorService.deleteMonitor(id);

        return ResponseEntity.noContent().build();
    }
}
