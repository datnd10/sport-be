package com.example.sport_be.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for {@code POST /api/admin/products}: creates a new
 * Sản_Phẩm (Product) together with its initial Biến_Thể_Sản_Phẩm
 * (ProductVariant) list (Requirement 1.2, 1.3, 1.6, 1.7).
 *
 * @param categoryId  the id of an existing Danh_Mục (Category); required
 * @param name        the product's name; required and must not be blank (Requirement 1.3)
 * @param description optional free-form description
 * @param basePrice   the product's base price; required and must be positive (Requirement 1.3)
 * @param variants    at least one initial variant to create alongside the product (Requirement 1.2, 1.6)
 */
public record ProductCreateRequest(
        @NotNull(message = "Danh_Mục không được để trống")
        UUID categoryId,

        @NotBlank(message = "Tên Sản_Phẩm không được để trống")
        String name,

        String description,

        @NotNull(message = "Giá Sản_Phẩm không được để trống")
        @Positive(message = "Giá Sản_Phẩm phải lớn hơn 0")
        BigDecimal basePrice,

        @NotEmpty(message = "Phải có ít nhất một Biến_Thể_Sản_Phẩm")
        @Valid
        List<ProductVariantRequest> variants
) {
}
