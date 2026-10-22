package com.example.sport_be.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic, Spring-independent pagination envelope returned by paginated
 * Admin list endpoints (e.g. {@code GET /api/admin/products},
 * Requirement 1.4), so controllers never leak {@link Page} directly in
 * their response type.
 *
 * @param content       the page's items
 * @param page          the current 0-based page index
 * @param size          the requested page size
 * @param totalElements the total number of items across every page
 * @param totalPages    the total number of pages
 * @param <T>           the type of item contained in the page
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /**
     * Builds a {@link PageResponse} from a Spring Data {@link Page},
     * mapping each element through {@code mapper} (e.g. entity to DTO).
     */
    public static <S, T> PageResponse<T> from(Page<S> source, java.util.function.Function<S, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages()
        );
    }
}
