package com.example.sport_be.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for {@code PUT /api/cart/items/{itemId}}.
 *
 * @param quantity the new quantity requested for the cart item (must be at
 *                  least 1); if it exceeds the variant's current stock, the
 *                  server clamps it down to the current stock quantity
 *                  instead of rejecting the request (Requirement 3.4).
 */
public record UpdateCartItemRequest(
        @NotNull(message = "quantity không được để trống")
        @Min(value = 1, message = "quantity phải lớn hơn hoặc bằng 1")
        Integer quantity
) {
}
