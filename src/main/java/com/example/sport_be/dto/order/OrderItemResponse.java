package com.example.sport_be.dto.order;

import com.example.sport_be.entity.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representation of a single {@link OrderItem} returned as part of an
 * {@link OrderResponse}, using the persisted snapshot fields so the
 * response remains accurate even if the referenced product is later
 * edited or soft-deleted (Requirement 1.5).
 *
 * @param variantId           the purchased Biến_Thể_Sản_Phẩm's id
 * @param productNameSnapshot the product name at the time of purchase
 * @param unitPriceSnapshot   the unit price at the time of purchase
 * @param quantity            the purchased quantity
 */
public record OrderItemResponse(
        UUID variantId,
        String productNameSnapshot,
        BigDecimal unitPriceSnapshot,
        int quantity,
        UUID id,
        UUID productId,
        String productName,
        String sku,
        String attributes,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String imagePath
) {

    public static OrderItemResponse from(OrderItem orderItem) {
        return from(orderItem, null);
    }

    public static OrderItemResponse from(OrderItem orderItem, String imagePath) {
        return new OrderItemResponse(
                orderItem.getVariant().getId(),
                orderItem.getProductNameSnapshot(),
                orderItem.getUnitPriceSnapshot(),
                orderItem.getQuantity(),
                orderItem.getId(),
                orderItem.getVariant().getProduct().getId(),
                orderItem.getProductNameSnapshot(),
                orderItem.getVariant().getSku(),
                orderItem.getVariant().getAttributes(),
                orderItem.getUnitPriceSnapshot(),
                orderItem.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(orderItem.getQuantity())),
                imagePath
        );
    }
}
