package com.example.sport_be.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single day's revenue point for the admin Dashboard's revenue trend
 * chart, for {@code GET /api/admin/dashboard/revenue-by-day}
 * (Requirement 10).
 *
 * <p>One entry is returned for every calendar day in the selected time
 * range, including days with zero revenue, so the frontend can render a
 * continuous time series without gaps.
 *
 * @param date    the calendar day (in the server's resolved time zone)
 * @param revenue the sum of {@code totalAmount} for Đơn_Hàng created on this day, excluding Đã_Hủy orders
 */
public record DailyRevenueResponse(
        LocalDate date,
        BigDecimal revenue
) {
}
