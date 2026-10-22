package com.example.sport_be.dto.inventory;

import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.StockStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Admin-facing representation of a {@link ProductVariant}'s inventory
 * state, used by {@code GET /api/admin/inventory} (Requirement 7.3, 7.4).
 *
 * @param variantId         the variant's unique identifier
 * @param sku               the variant's SKU
 * @param productId         the id of the product this variant belongs to
 * @param productName       the name of the product this variant belongs to
 * @param attributes        the variant's distinguishing attributes (size/color/...)
 * @param stockQuantity     the current stock quantity
 * @param lowStockThreshold the threshold below which the variant is considered low on stock
 * @param stockStatus       the derived stock status (Property 27)
 */
public record InventoryItemResponse(
        UUID variantId,
        String sku,
        UUID productId,
        String productName,
        String attributes,
        BigDecimal price,
        int stockQuantity,
        int lowStockThreshold,
        StockStatus stockStatus
) {

    public static InventoryItemResponse from(ProductVariant variant) {
        return new InventoryItemResponse(
                variant.getId(),
                variant.getSku(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                variant.getAttributes(),
                variant.getPrice(),
                variant.getStockQuantity(),
                variant.getLowStockThreshold(),
                StockStatus.of(variant)
        );
    }
}
