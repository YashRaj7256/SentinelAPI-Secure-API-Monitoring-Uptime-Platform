package com.sentinelapi.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sentinelapi.dto.CreateMonitorRequest;
import com.sentinelapi.dto.MonitorResponse;
import com.sentinelapi.dto.PageResponse;
import com.sentinelapi.dto.PatchMonitorRequest;
import com.sentinelapi.entity.HttpMethod;
import com.sentinelapi.entity.Monitor;
import com.sentinelapi.entity.MonitorStatus;
import com.sentinelapi.exception.ResourceNotFoundException;
import com.sentinelapi.mapper.MonitorMapper;
import com.sentinelapi.repository.MonitorRepository;

/**
 * Service layer for Monitor CRUD operations and business logic.
 *
 * <h3>Architecture: Controller → Service → Repository</h3>
 * <pre>
 *   HTTP Request
 *       ↓
 *   Controller  (HTTP concerns: request/response, status codes)
 *       ↓
 *   Service     (Business logic: validation, rules, orchestration)
 *       ↓
 *   Repository  (Data access: SQL queries, persistence)
 *       ↓
 *   Database
 * </pre>
 *
 * <p>The service layer is the <strong>only place</strong> where business logic
 * should live. Controllers should be thin — they delegate to services.
 * Repositories should be thin — they handle data access only.
 *
 * <h3>Constructor Dependency Injection</h3>
 * <p>Spring injects dependencies via the constructor. When a class has exactly
 * one constructor, {@code @Autowired} is optional — Spring infers it.
 * Constructor injection is preferred over field injection ({@code @Autowired}
 * on fields) because:
 * <ul>
 *   <li>Dependencies are visible in the constructor signature</li>
 *   <li>Fields can be {@code final} — immutable after construction</li>
 *   <li>The class can't be instantiated without its dependencies (fail-fast)</li>
 *   <li>Easier to test — just pass mocks to the constructor</li>
 * </ul>
 *
 * <h3>{@code @Transactional}</h3>
 * <p>Class-level {@code @Transactional(readOnly = true)} means all methods
 * in this class run inside a read-only database transaction by default.
 * Methods that modify data override this with {@code @Transactional}
 * (which defaults to read-write).
 *
 * <p><strong>Why readOnly = true as default?</strong> Most operations in a
 * typical service are reads. Read-only transactions enable database optimizations
 * (no write-ahead log, no lock escalation). We explicitly opt into writes
 * only when needed.
 */
@Service
@Transactional(readOnly = true)
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);

    private final MonitorRepository monitorRepository;
    private final MonitorMapper monitorMapper;

    /**
     * Constructor — Spring injects the repository and mapper automatically.
     *
     * <p>No {@code @Autowired} needed — Spring auto-detects the single constructor.
     * Both dependencies are {@code final} — they can't be changed after construction.
     *
     * <p><strong>How does Spring know what to inject?</strong> Spring scans the
     * classpath for beans that match the parameter types. It finds
     * {@code MonitorRepository} (auto-implemented by Spring Data) and
     * {@code MonitorMapper} (annotated with {@code @Component}).
     */
    public MonitorService(MonitorRepository monitorRepository, MonitorMapper monitorMapper) {
        this.monitorRepository = monitorRepository;
        this.monitorMapper = monitorMapper;
    }

    // =========================================================================
    // CREATE
    // =========================================================================

    /**
     * Creates a new monitor from a validated request.
     *
     * <p>{@code @Transactional} (without readOnly) overrides the class-level
     * readOnly default. This method modifies the database — it needs a
     * read-write transaction.
     *
     * <p>Flow:
     * <ol>
     *   <li>Convert DTO → entity via mapper (includes httpMethod validation)</li>
     *   <li>Persist the entity via repository</li>
     *   <li>Convert saved entity → response DTO</li>
     * </ol>
     *
     * @param request the validated create request
     * @return the created monitor as a response DTO
     * @throws IllegalArgumentException if httpMethod is invalid (e.g., "POST")
     */
    @Transactional
    public MonitorResponse createMonitor(CreateMonitorRequest request) {
        log.info("Creating monitor: name='{}', url='{}'", request.getName(), request.getUrl());

        Monitor monitor = monitorMapper.toEntity(request);

        // repository.save() performs an INSERT (new entity, no ID yet).
        // After save(), the entity's ID is populated by PostgreSQL.
        Monitor saved = monitorRepository.save(monitor);

        log.info("Monitor created: id={}, name='{}'", saved.getId(), saved.getName());

        return monitorMapper.toResponse(saved);
    }

    // =========================================================================
    // READ
    // =========================================================================

    /**
     * Retrieves all monitors with pagination.
     *
     * <p><strong>Why pagination?</strong> Without it, if you have 10,000 monitors,
     * {@code findAll()} would load all 10,000 into memory and serialize them
     * to a massive JSON response. Pagination limits each request to a
     * manageable page size.
     *
     * <p>{@code PageRequest.of(page, size, sort)} creates a {@code Pageable}
     * object that Spring Data uses to generate the SQL:
     * {@code SELECT * FROM monitors ORDER BY created_at DESC LIMIT size OFFSET page*size}
     *
     * @param page the page number (0-based)
     * @param size the page size (capped at 100)
     * @return a paginated response containing monitor DTOs
     */
    public PageResponse<MonitorResponse> getAllMonitors(int page, int size) {
        // Cap page size at 100 to prevent abuse.
        // Math.min returns the smaller of the two values.
        int cappedSize = Math.min(size, 100);

        // Sort by creation date, newest first.
        // This ensures consistent ordering across pages.
        Pageable pageable = PageRequest.of(page, cappedSize, Sort.by("createdAt").descending());

        // Spring Data generates: SELECT * FROM monitors ORDER BY created_at DESC LIMIT ? OFFSET ?
        Page<Monitor> monitorPage = monitorRepository.findAll(pageable);

        // Convert each entity in the page to a response DTO.
        // .map() applies the mapper to each element — this is the Stream pattern.
        List<MonitorResponse> content = monitorPage.getContent()
                .stream()
                .map(monitorMapper::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                monitorPage.getNumber(),      // current page number
                monitorPage.getSize(),         // page size
                monitorPage.getTotalElements(),// total items across all pages
                monitorPage.getTotalPages()    // total pages
        );
    }

    /**
     * Retrieves a single monitor by its ID.
     *
     * <p>{@code findById()} returns {@code Optional<Monitor>}. We use
     * {@code orElseThrow()} to convert the empty case to a clear exception.
     *
     * <p><strong>Why Optional?</strong> It forces you to handle the "not found"
     * case explicitly. Without it, you'd get {@code null} and risk a
     * {@code NullPointerException} somewhere downstream — a much worse error.
     *
     * @param id the monitor ID
     * @return the monitor as a response DTO
     * @throws ResourceNotFoundException if no monitor exists with the given ID
     */
    public MonitorResponse getMonitorById(Long id) {
        Monitor monitor = monitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor", id));

        return monitorMapper.toResponse(monitor);
    }

    // =========================================================================
    // UPDATE (PATCH)
    // =========================================================================

    /**
     * Partially updates a monitor — only provided (non-null) fields are changed.
     *
     * <p>This implements HTTP PATCH semantics:
     * <pre>
     * PATCH /api/v1/monitors/1
     * { "checkInterval": 600 }
     * </pre>
     * Only {@code checkInterval} is updated. All other fields remain unchanged.
     *
     * <p><strong>Implementation pattern:</strong> For each field in the request,
     * check if it's non-null (meaning the client provided it), and if so,
     * update it on the entity. This is explicit and readable.
     *
     * <p><strong>Alternative approaches (and why we don't use them):</strong>
     * <ul>
     *   <li>Reflection-based patching — clever but fragile, hard to debug</li>
     *   <li>JSON Patch (RFC 6902) — formal but over-complex for our use case</li>
     *   <li>JSON Merge Patch (RFC 7386) — simpler but can't distinguish
     *       "field absent" from "field set to null"</li>
     * </ul>
     *
     * @param id      the monitor ID
     * @param request the patch request with only the fields to update
     * @return the fully updated monitor as a response DTO
     * @throws ResourceNotFoundException  if no monitor exists with the given ID
     * @throws IllegalArgumentException   if httpMethod is invalid
     */
    @Transactional
    public MonitorResponse patchMonitor(Long id, PatchMonitorRequest request) {
        Monitor monitor = monitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor", id));

        log.info("Patching monitor: id={}", id);

        // Apply each non-null field from the request to the entity.
        // This is the standard PATCH pattern in Spring Boot.

        if (request.getName() != null) {
            monitor.setName(request.getName());
        }

        if (request.getUrl() != null) {
            monitor.setUrl(request.getUrl());
        }

        if (request.getHttpMethod() != null) {
            // valueOf() throws IllegalArgumentException for invalid methods.
            // The global exception handler will convert this to a 400 response.
            monitor.setHttpMethod(HttpMethod.valueOf(request.getHttpMethod().toUpperCase()));
        }

        if (request.getExpectedStatus() != null) {
            monitor.setExpectedStatus(request.getExpectedStatus());
        }

        if (request.getCheckInterval() != null) {
            monitor.setCheckInterval(request.getCheckInterval());
        }

        if (request.getTimeout() != null) {
            monitor.setTimeout(request.getTimeout());
        }

        if (request.getActive() != null) {
            // When deactivating, set status to PAUSED.
            // When reactivating, reset to UNKNOWN so it gets re-evaluated.
            if (!request.getActive()) {
                monitor.setStatus(MonitorStatus.PAUSED);
            } else if (!monitor.getActive()) {
                // Was inactive, now being reactivated
                monitor.setStatus(MonitorStatus.UNKNOWN);
                monitor.setConsecutiveFailures(0);
            }
            monitor.setActive(request.getActive());
        }

        if (request.getFailureThreshold() != null) {
            monitor.setFailureThreshold(request.getFailureThreshold());
        }

        // Always update the timestamp on modification.
        monitor.setUpdatedAt(Instant.now());

        // repository.save() performs an UPDATE (entity already has an ID).
        // Hibernate detects the existing ID and generates UPDATE instead of INSERT.
        Monitor saved = monitorRepository.save(monitor);

        log.info("Monitor patched: id={}, name='{}'", saved.getId(), saved.getName());

        return monitorMapper.toResponse(saved);
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    /**
     * Deletes a monitor by its ID.
     *
     * <p>Due to {@code ON DELETE CASCADE} in the schema, deleting a monitor
     * also deletes all its associated {@code monitor_results} rows.
     *
     * <p>We first check that the monitor exists to provide a clear 404 error
     * instead of silently doing nothing (which is what {@code deleteById()}
     * does when the ID doesn't exist).
     *
     * @param id the monitor ID
     * @throws ResourceNotFoundException if no monitor exists with the given ID
     */
    @Transactional
    public void deleteMonitor(Long id) {
        if (!monitorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Monitor", id);
        }

        log.info("Deleting monitor: id={}", id);

        monitorRepository.deleteById(id);

        log.info("Monitor deleted: id={}", id);
    }
}
