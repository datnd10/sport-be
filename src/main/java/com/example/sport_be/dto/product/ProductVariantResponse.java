package com.example.sport_be.dto.product;

import com.example.sport_be.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representation of a Biến_Thể_Sản_Phẩm (ProductVariant) returned as part
 * of a {@link ProductResponse} (Requirement 1.2, 1.6).
 *
 * @param id                the variant's unique identifier
 * @param sku               the variant's SKU
 * @param attributes        the variant's distinguishing attributes (size/color/...)
 * @param price             the variant's price
 * @param stockQuantity     the variant's current stock quantity
 * @param lowStockThreshold the low-stock warning threshold
 */
public record ProductVariantResponse(
        UUID id,
        String sku,
        String attributes,
        BigDecimal price,
        int stockQuantity,
        int lowStockThreshold
) {

    public static ProductVariantResponse from(ProductVariant variant) {
        return new ProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getAttributes(),
                variant.getPrice(),
                variant.getStockQuantity(),
                variant.getLowStockThreshold()
        );
    }
}
