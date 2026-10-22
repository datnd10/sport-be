package com.example.sport_be.dto.dashboard;

import com.example.sport_be.enums.OrderStatus;

/**
 * A single entry in the admin Dashboard's order-status breakdown,
 * nested inside {@link DashboardSummaryResponse} (Requirement 10).
 *
 * @param status the Trạng_Thái_Đơn_Hàng this entry counts
 * @param count  the number of Đơn_Hàng with this status within the selected time range
 */
public record OrderStatusBreakdownResponse(
        OrderStatus status,
        long count
) {
}
