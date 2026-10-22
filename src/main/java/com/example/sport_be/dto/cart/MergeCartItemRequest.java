package com.example.sport_be.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * A single Biến_Thể_Sản_Phẩm/quantity pair from the Khách_Hàng's temporary
 * (local/guest) Giỏ_Hàng, submitted as part of {@code POST
 * /api/cart/merge} to be merged into the Account's server-side Giỏ_Hàng
 * (Requirement 3.6).
 *
 * @param variantId the Biến_Thể_Sản_Phẩm present in the local cart
 * @param quantity  the quantity requested for that variant in the local
 *                  cart (must be at least 1)
 */
public record MergeCartItemRequest(
        @NotNull(message = "variantId không được để trống")
        UUID variantId,

        @NotNull(message = "quantity không được để trống")
        @Min(value = 1, message = "quantity phải lớn hơn hoặc bằng 1")
        Integer quantity
) {
}
