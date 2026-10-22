package com.example.sport_be.dto.cart;

import com.example.sport_be.entity.Cart;
import com.example.sport_be.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Representation of a Giỏ_Hàng (Cart) and its items, returned by {@code
 * GET /api/cart} and {@code POST /api/cart/items} (Requirement 3.1, 3.3).
 *
 * @param id          the cart's unique identifier
 * @param items       the cart's line items
 * @param totalAmount the sum of every item's line total
 */
public record CartResponse(
        UUID id,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {

    public static CartResponse from(Cart cart, List<CartItem> cartItems) {
        List<CartItemResponse> items = cartItems.stream()
                .map(CartItemResponse::from)
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), items, totalAmount);
    }
}
