package com.example.sport_be.dto.order;

import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representation of a newly created (or looked up) Đơn_Hàng (Order),
 * returned by {@code POST /api/orders} as the order confirmation
 * response, together with the order's human-readable {@code orderCode}
 * for the Khách_Hàng to reference (Requirement 4.8).
 *
 * @param id               the order's unique identifier
 * @param orderCode        the human-readable order code shown to the customer on the confirmation screen
 * @param status           the order's current Trạng_Thái_Đơn_Hàng
 * @param paymentMethod    the chosen Phương_Thức_Thanh_Toán
 * @param subtotal         the sum of (price × quantity) across all items, before discount
 * @param discountAmount   the amount deducted by the applied Mã_Giảm_Giá, if any
 * @param totalAmount      the final amount due (subtotal − discountAmount)
 * @param recipientName    the shipping recipient's name
 * @param recipientPhone   the shipping recipient's phone number
 * @param shippingAddress  the shipping destination address
 * @param items            the order's line items
 * @param bankTransferInfo the shop's bank account/QR info, present only when
 *                         {@code paymentMethod} is {@code BANK_TRANSFER} (Requirement 4.4);
 *                         {@code null} for COD orders, which are recorded immediately with
 *                         no payment confirmation step required (Requirement 4.5)
 * @param createdAt        when the order was placed
 */
public record OrderResponse(
        UUID id,
        String orderCode,
        OrderStatus status,
        PaymentMethod paymentMethod,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String recipientName,
        String recipientPhone,
        String shippingAddress,
        List<OrderItemResponse> items,
        BankTransferInfoResponse bankTransferInfo,
        Instant createdAt
) {

    public static OrderResponse from(Order order, List<OrderItem> orderItems) {
        return from(order, orderItems, null);
    }

    public static OrderResponse from(Order order, List<OrderItem> orderItems,
                                      BankTransferInfoResponse bankTransferInfo) {
        List<OrderItemResponse> items = orderItems.stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getShippingAddress(),
                items,
                bankTransferInfo,
                order.getCreatedAt()
        );
    }
}
