package com.example.sport_be.enums;

import com.example.sport_be.entity.ProductVariant;

/**
 * Stock level classification for a {@link ProductVariant}, derived purely
 * from {@code stockQuantity} and {@code lowStockThreshold} (Requirement
 * 7.3, 7.4).
 *
 * <ul>
 *   <li>{@link #OUT_OF_STOCK} when {@code stockQuantity == 0}</li>
 *   <li>{@link #LOW_STOCK} when {@code 0 < stockQuantity < lowStockThreshold}</li>
 *   <li>{@link #IN_STOCK} otherwise</li>
 * </ul>
 */
public enum StockStatus {
    OUT_OF_STOCK,
    LOW_STOCK,
    IN_STOCK;

    /**
     * Derives the {@link StockStatus} of a variant from its current
     * {@code stockQuantity} and {@code lowStockThreshold} (Property 27).
     */
    public static StockStatus of(int stockQuantity, int lowStockThreshold) {
        if (stockQuantity == 0) {
            return OUT_OF_STOCK;
        }
        if (stockQuantity < lowStockThreshold) {
            return LOW_STOCK;
        }
        return IN_STOCK;
    }

    public static StockStatus of(ProductVariant variant) {
        return of(variant.getStockQuantity(), variant.getLowStockThreshold());
    }
}
