package com.example.sport_be.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request payload for {@code POST /api/admin/products/{id}/variants}: adds
 * a new Biến_Thể_Sản_Phẩm (ProductVariant) to an existing Sản_Phẩm
 * (Requirement 1.6, 1.7).
 *
 * @param sku               the variant's SKU; required and must be unique across all variants (Requirement 1.7)
 * @param attributes        free-form distinguishing attributes (size/color/...); optional
 * @param price             the variant's price; required and must be positive
 * @param stockQuantity     the initial stock quantity; required and must not be negative
 * @param lowStockThreshold the low-stock warning threshold; optional (defaults handled by the entity if null)
 */
public record AddProductVariantRequest(
        @NotBlank(message = "SKU không được để trống")
        String sku,

        String attributes,

        @NotNull(message = "Giá Biến_Thể_Sản_Phẩm không được để trống")
        @Positive(message = "Giá Biến_Thể_Sản_Phẩm phải lớn hơn 0")
        BigDecimal price,

        @NotNull(message = "Số lượng Tồn_Kho khởi tạo không được để trống")
        @Min(value = 0, message = "Số lượng Tồn_Kho khởi tạo không được âm")
        Integer stockQuantity,

        Integer lowStockThreshold
) {
}
