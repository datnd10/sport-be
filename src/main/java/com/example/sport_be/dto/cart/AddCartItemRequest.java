package com.example.sport_be.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload for {@code POST /api/cart/items}.
 *
 * @param variantId the Biến_Thể_Sản_Phẩm to add to the Giỏ_Hàng
 * @param quantity  the quantity to add (must be at least 1)
 */
public record AddCartItemRequest(
        @NotNull(message = "variantId không được để trống")
        UUID variantId,

        @NotNull(message = "quantity không được để trống")
        @Min(value = 1, message = "quantity phải lớn hơn hoặc bằng 1")
        Integer quantity
) {
}
