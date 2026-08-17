package com.sentinelapi.dto;

import java.util.List;

import lombok.Getter;

/**
 * Generic wrapper for paginated API responses.
 *
 * <h3>Why Not Use Spring's {@code Page<T>} Directly?</h3>
 * <p>Spring Data's {@code Page} serializes to JSON with 15+ fields including
 * internal details like {@code sort.sorted}, {@code pageable.offset},
 * {@code numberOfElements}, {@code first}, {@code last}, {@code empty}, etc.
 * This is noisy and leaks Spring internals into the API contract.
 *
 * <p>Our response includes only what API consumers need:
 * <pre>
 * {
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 47,
 *   "totalPages": 3
 * }
 * </pre>
 *
 * <h3>Java Generics</h3>
 * <p>The {@code <T>} type parameter makes this class reusable for any entity:
 * {@code PageResponse<MonitorResponse>}, {@code PageResponse<MonitorResultResponse>}, etc.
 * The compiler ensures type safety — you can't accidentally mix types.
 *
 * @param <T> the type of content items in the page
 */
@Getter
public class PageResponse<T> {

    /** The items on this page. */
    private final List<T> content;

    /** Current page number (0-based). */
    private final int page;

    /** Requested page size. */
    private final int size;

    /** Total number of items across all pages. */
    private final long totalElements;

    /** Total number of pages. */
    private final int totalPages;

    /**
     * Constructs a PageResponse.
     *
     * <p>All fields are {@code final} — once created, a PageResponse is immutable.
     * Immutability is a good practice for response objects because they should
     * represent a snapshot of data, not a mutable state that could be changed later.
     *
     * @param content       the items on this page
     * @param page          current page number (0-based)
     * @param size          requested page size
     * @param totalElements total number of items across all pages
     * @param totalPages    total number of pages
     */
    public PageResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
}
