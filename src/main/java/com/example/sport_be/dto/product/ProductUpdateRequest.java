package com.example.sport_be.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request payload for {@code PUT /api/admin/products/{id}}: updates an
 * existing Sản_Phẩm's (Product) own fields (Requirement 1.3, 1.4).
 *
 * <p>Does not touch variants or {@code stockQuantity}; stock changes must
 * go through {@code InventoryService}.
 *
 * @param categoryId  the id of an existing Danh_Mục (Category); required
 * @param name        the product's new name; required and must not be blank (Requirement 1.3)
 * @param description optional free-form description
 * @param basePrice   the product's new base price; required and must be positive (Requirement 1.3)
 */
public record ProductUpdateRequest(
        @NotNull(message = "Danh_Mục không được để trống")
        UUID categoryId,

        @NotBlank(message = "Tên Sản_Phẩm không được để trống")
        String name,

        String description,

        @NotNull(message = "Giá Sản_Phẩm không được để trống")
        @Positive(message = "Giá Sản_Phẩm phải lớn hơn 0")
        BigDecimal basePrice
) {
}
