package com.example.sport_be.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request payload for {@code PUT /api/admin/products/{id}/variants/{variantId}}:
 * updates an existing Biến_Thể_Sản_Phẩm (ProductVariant)'s own fields.
 *
 * <p>Deliberately has no {@code stockQuantity} field: Tồn_Kho (stock)
 * changes must go exclusively through {@code InventoryService} (via
 * {@code PUT /api/admin/inventory/{variantId}}) so every stock change is
 * captured in {@code InventoryHistory} (Requirement 7.5). This endpoint
 * must never be able to bypass that audit trail.
 *
 * @param sku               the variant's new SKU; required and must stay unique across all variants
 * @param attributes        free-form distinguishing attributes (size/color/...); optional
 * @param price             the variant's new price; required and must be positive
 * @param lowStockThreshold the low-stock warning threshold; optional (existing value is kept if null)
 */
public record UpdateProductVariantRequest(
        @NotBlank(message = "SKU không được để trống")
        String sku,

        String attributes,

        @NotNull(message = "Giá Biến_Thể_Sản_Phẩm không được để trống")
        @Positive(message = "Giá Biến_Thể_Sản_Phẩm phải lớn hơn 0")
        BigDecimal price,

        Integer lowStockThreshold
) {
}
