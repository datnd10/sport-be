package com.example.sport_be.dto.order;

import com.example.sport_be.entity.Order;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight summary representation of an {@link Order}, used for list
 * views: the Customer's own order history ({@code GET /api/orders},
 * Requirement 5.1) and the Admin's full order list
 * ({@code GET /api/admin/orders}, Requirement 5.2).
 *
 * <p>{@code accountEmail} and {@code accountFullName} are {@code null}
 * for guest orders (no associated {@link com.example.sport_be.entity.Account}); they are
 * populated so the Admin list view can show customer info alongside each
 * order (Requirement 5.2).
 *
 * @param id              the order's unique identifier
 * @param orderCode       the human-readable order code
 * @param status          the order's current Trạng_Thái_Đơn_Hàng
 * @param paymentMethod   the chosen Phương_Thức_Thanh_Toán
 * @param totalAmount     the final amount due
 * @param accountEmail    the placing Account's email, or {@code null} for a guest order
 * @param accountFullName the placing Account's full name, or {@code null} for a guest order
 * @param createdAt       when the order was placed
 */
public record OrderSummaryResponse(
        UUID id,
        String orderCode,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal totalAmount,
        String accountEmail,
        String accountFullName,
        Instant createdAt
) {

    public static OrderSummaryResponse from(Order order) {
        boolean hasAccount = order.getAccount() != null;
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderCode(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                hasAccount ? order.getAccount().getEmail() : null,
                hasAccount ? order.getAccount().getFullName() : null,
                order.getCreatedAt()
        );
    }
}
