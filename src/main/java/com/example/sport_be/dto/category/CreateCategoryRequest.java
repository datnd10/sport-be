package com.example.sport_be.dto.category;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for {@code POST /api/admin/categories} (Requirement 1.1).
 *
 * @param name the new category's name; required and must not be blank
 */
public record CreateCategoryRequest(
        @NotBlank(message = "Tên Danh_Mục không được để trống")
        String name
) {
}
