package com.example.sport_be.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * A single Biến_Thể_Sản_Phẩm/quantity pair submitted as part of
 * {@code POST /api/orders}, identifying one line item to purchase
 * (Requirement 4.1, 4.2).
 *
 * @param variantId the Biến_Thể_Sản_Phẩm to purchase
 * @param quantity  the quantity to purchase (must be at least 1)
 */
public record OrderItemRequest(
        @NotNull(message = "variantId không được để trống")
        UUID variantId,

        @NotNull(message = "quantity không được để trống")
        @Min(value = 1, message = "quantity phải lớn hơn hoặc bằng 1")
        Integer quantity
) {
}
