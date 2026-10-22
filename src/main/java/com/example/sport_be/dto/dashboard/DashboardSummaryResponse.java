package com.example.sport_be.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin-facing summary shown on the Dashboard overview page, for
 * {@code GET /api/admin/dashboard/summary} (Requirement 10.1, 10.2).
 *
 * @param totalOrders          the number of Đơn_Hàng (any status) created within the selected time range (Property 32)
 * @param totalRevenue         the sum of {@code totalAmount} for Đơn_Hàng in the same range, excluding Đã_Hủy orders (Property 32)
 * @param lowStockProductCount the current number of Biến_Thể_Sản_Phẩm that are low on stock (Property 27), independent of the time range
 * @param averageOrderValue    {@code totalRevenue} divided by the number of non-cancelled Đơn_Hàng in the range (0 when there are none)
 * @param newCustomerCount     the number of Tài_Khoản with role CUSTOMER created within the selected time range
 * @param cancelledOrderCount  the number of Đơn_Hàng in the range with status Đã_Hủy (CANCELLED)
 * @param statusBreakdown      the count of Đơn_Hàng in the range grouped by {@code OrderStatus}, one entry per status that has at least one order
 */
public record DashboardSummaryResponse(
        long totalOrders,
        BigDecimal totalRevenue,
        long lowStockProductCount,
        BigDecimal averageOrderValue,
        long newCustomerCount,
        long cancelledOrderCount,
        List<OrderStatusBreakdownResponse> statusBreakdown
) {
}
