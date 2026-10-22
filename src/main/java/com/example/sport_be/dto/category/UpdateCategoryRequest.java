package com.example.sport_be.dto.category;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for {@code PUT /api/admin/categories/{id}}, used to
 * rename an existing Danh_Mục (Category).
 *
 * @param name the Danh_Mục's new name; required and must not be blank
 */
public record UpdateCategoryRequest(
        @NotBlank(message = "Tên Danh_Mục không được để trống")
        String name
) {
}
