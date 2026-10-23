package com.example.sport_be.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request payload for {@code POST /api/products/{id}/reviews}: submits a
 * Đánh_Giá_Sản_Phẩm (Product_Review) for a product the customer has
 * purchased (Requirement 9.1, 9.2).
 *
 * @param orderItemId the specific purchased {@code OrderItem} being
 *                     reviewed; optional — when omitted, the first
 *                     eligible, not-yet-reviewed order item for this
 *                     product is used instead
 * @param rating       the review's rating; required, 1 to 5 inclusive
 * @param comment      optional free-text comment
 */
public record CreateReviewRequest(
        UUID orderItemId,

        @NotNull(message = "rating không được để trống")
        @Min(value = 1, message = "rating phải từ 1 đến 5")
        @Max(value = 5, message = "rating phải từ 1 đến 5")
        Integer rating,

        @jakarta.validation.constraints.Size(max = 5000, message = "Nhận xét không được vượt quá 5000 ký tự")
        String comment
) {
}
