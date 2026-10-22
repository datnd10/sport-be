package com.example.sport_be.dto.order;

import com.example.sport_be.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for {@code PUT /api/admin/orders/{id}/status}: the
 * target Trạng_Thái_Đơn_Hàng the Admin wants to transition the order to
 * (Requirement 5.3, 5.5).
 *
 * @param status the target status; {@code OrderStatusService} rejects
 *               the request if the transition from the order's current
 *               status to this one is not allowed
 */
public record UpdateOrderStatusRequest(
        @NotNull(message = "Trạng_Thái_Đơn_Hàng mới không được để trống")
        OrderStatus status
) {
}
