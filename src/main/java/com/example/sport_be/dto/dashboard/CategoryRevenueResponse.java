package com.example.sport_be.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single entry in the admin Dashboard's revenue-by-category
 * breakdown, for {@code GET /api/admin/dashboard/revenue-by-category}
 * (Requirement 10).
 *
 * @param categoryId   the Danh_Mục's unique identifier
 * @param categoryName the Danh_Mục's name
 * @param revenue      the sum of {@code quantity * unitPriceSnapshot} across every
 *                      Đơn_Hàng_Chi_Tiết (order item) for products in this category,
 *                      within the selected time range, excluding Đã_Hủy orders
 */
public record CategoryRevenueResponse(
        UUID categoryId,
        String categoryName,
        BigDecimal revenue
) {
}
