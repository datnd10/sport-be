package com.example.sport_be.dto.cart;

import com.example.sport_be.entity.CartItem;
import com.example.sport_be.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representation of a single line item within a {@link
 * com.example.sport_be.entity.Cart}, returned as part of a {@link
 * CartResponse} (Requirement 3.1, 3.3).
 *
 * @param id            the cart item's unique identifier
 * @param variantId     the Biến_Thể_Sản_Phẩm being purchased
 * @param productName   the name of the Sản_Phẩm the variant belongs to
 * @param sku           the variant's SKU
 * @param attributes    the variant's distinguishing attributes (size/color/...)
 * @param unitPrice     the variant's current price
 * @param quantity      the quantity requested
 * @param stockQuantity the variant's current stock quantity
 * @param lineTotal     {@code unitPrice * quantity}
 */
public record CartItemResponse(
        UUID id,
        UUID variantId,
        String productName,
        String sku,
        String attributes,
        BigDecimal unitPrice,
        int quantity,
        int stockQuantity,
        BigDecimal lineTotal,
        UUID productId,
        String imagePath
) {

    public CartItemResponse withImage(String path) {
        return new CartItemResponse(id, variantId, productName, sku, attributes, unitPrice, quantity, stockQuantity, lineTotal, productId, path);
    }

    public static CartItemResponse from(CartItem item) {
        ProductVariant variant = item.getVariant();
        BigDecimal unitPrice = variant.getPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return new CartItemResponse(
                item.getId(),
                variant.getId(),
                variant.getProduct().getName(),
                variant.getSku(),
                variant.getAttributes(),
                unitPrice,
                item.getQuantity(),
                variant.getStockQuantity(),
                lineTotal,
                variant.getProduct().getId(),
                null
        );
    }
}
