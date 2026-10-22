package com.example.sport_be.dto.category;

import com.example.sport_be.entity.Category;

import java.util.UUID;

/**
 * Public-facing representation of a {@link Category}, used by
 * {@code GET /api/categories} and {@code POST /api/admin/categories}
 * (Requirement 1.1).
 *
 * @param id   the category's unique identifier
 * @param name the category's human-readable name
 * @param slug the category's unique URL-friendly slug
 */
public record CategoryResponse(
        UUID id,
        String name,
        String slug
) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug()
        );
    }
}
