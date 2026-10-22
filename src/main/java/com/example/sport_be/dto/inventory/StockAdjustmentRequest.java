package com.example.sport_be.dto.inventory;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for {@code PUT /api/admin/inventory/{variantId}}: sets
 * the variant's stock quantity to an absolute new value (Requirement 7.1,
 * 7.2).
 *
 * @param newQuantity the new absolute stock quantity; rejected by
 *                     {@code InventoryService} if negative
 */
public record StockAdjustmentRequest(
        @NotNull(message = "Số lượng Tồn_Kho mới không được để trống")
        Integer newQuantity
) {
}
