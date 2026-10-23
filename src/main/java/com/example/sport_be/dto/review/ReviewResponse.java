package com.example.sport_be.dto.review;

import com.example.sport_be.entity.ProductReview;

import java.time.Instant;
import java.util.UUID;

/**
 * Representation of a saved Đánh_Giá_Sản_Phẩm (Product_Review), returned
 * by {@code POST /api/products/{id}/reviews}, {@code GET
 * /api/products/{id}/reviews} and {@code GET /api/admin/reviews}
 * (Requirement 9.1, 9.3, 9.4).
 *
 * @param id             the review's unique identifier
 * @param productId      the reviewed Sản_Phẩm's id
 * @param productName    the reviewed Sản_Phẩm's name, for display on the
 *                       Admin review management page
 * @param accountId      the reviewing Khách_Hàng's Account id
 * @param reviewerName   the reviewing Khách_Hàng's display name, may be
 *                       {@code null}
 * @param rating         the review's rating
 * @param comment        the review's free-text comment, may be {@code null}
 * @param hidden         whether an admin has hidden this review
 *                       (Requirement 9.4)
 * @param createdAt      the instant the review was created
 */
public record ReviewResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID accountId,
        String reviewerName,
        int rating,
        String comment,
        boolean hidden,
        Instant createdAt,
        java.util.List<ReviewImageResponse> images
) {

    public static ReviewResponse from(ProductReview review) {
        return new ReviewResponse(
                review.getId(),
                review.getProduct().getId(),
                review.getProduct().getName(),
                review.getAccount().getId(),
                review.getAccount().getFullName(),
                review.getRating(),
                review.getComment(),
                review.isHidden(),
                review.getCreatedAt(),
                review.getImages().stream().map(ReviewImageResponse::from).toList()
        );
    }
}
