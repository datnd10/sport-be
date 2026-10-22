package com.example.sport_be.dto.review;

import java.util.List;

/**
 * Result of {@code GET /api/products/{id}/reviews}: the non-hidden
 * Đánh_Giá_Sản_Phẩm for a Sản_Phẩm, together with the average rating and
 * total count computed only over those non-hidden reviews (Requirement
 * 9.3). Reviews hidden by an admin (Requirement 9.4) are excluded from
 * both the list and the average.
 *
 * @param reviews       the non-hidden reviews for the product
 * @param averageRating the arithmetic mean of {@code rating} across
 *                       {@code reviews}; {@code 0} when there are no
 *                       non-hidden reviews
 * @param totalCount    the number of non-hidden reviews
 */
public record ProductReviewListResponse(
        List<ReviewResponse> reviews,
        double averageRating,
        long totalCount
) {
}
