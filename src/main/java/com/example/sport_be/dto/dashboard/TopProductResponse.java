package com.example.sport_be.dto.dashboard;

import java.util.UUID;

/**
 * A single entry in the admin Dashboard's best-selling products list,
 * for {@code GET /api/admin/dashboard/top-products} (Requirement 10.3).
 *
 * @param productId         the Sản_Phẩm's unique identifier
 * @param productName       the Sản_Phẩm's name
 * @param totalQuantitySold the total quantity sold across all Biến_Thể_Sản_Phẩm
 *                          of this product within the selected time range,
 *                          excluding Đã_Hủy orders (Property 34)
 */
public record TopProductResponse(
        UUID productId,
        String productName,
        long totalQuantitySold
) {
}
