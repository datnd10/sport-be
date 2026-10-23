package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.review.CreateReviewRequest;
import com.example.sport_be.dto.review.ProductReviewListResponse;
import com.example.sport_be.dto.review.ReviewResponse;
import com.example.sport_be.entity.ProductReview;
import com.example.sport_be.enums.Role;
import com.example.sport_be.exception.ForbiddenException;
import com.example.sport_be.security.JwtPrincipal;
import com.example.sport_be.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints for submitting and viewing Đánh_Giá_Sản_Phẩm (Product_Review)
 * records (Requirement 9).
 *
 * <p>{@code POST /api/products/{id}/reviews} falls outside the public GET
 * matcher for {@code /api/products/**} in {@code SecurityConfig}, so it
 * already requires an authenticated caller; this controller additionally
 * restricts it to {@code ROLE_CUSTOMER} since only Khách_Hàng accounts
 * place and review orders. {@code GET /api/products/{id}/reviews} is
 * covered by that same public GET matcher, so it is open to anonymous
 * callers (Requirement 9.3). {@code DELETE /api/admin/reviews/{id}} falls
 * under the {@code /api/admin/**} matcher and requires {@code
 * ROLE_ADMIN} (Requirement 9.4).
 */
@RestController
public class ReviewController {

    private static final String CUSTOMER_ONLY_MESSAGE = "Chỉ Khách_Hàng mới có thể gửi Đánh_Giá_Sản_Phẩm.";

    private final ReviewService reviewService;
    private final com.example.sport_be.service.ReviewSubmissionService reviewSubmissionService;

    public ReviewController(ReviewService reviewService, com.example.sport_be.service.ReviewSubmissionService reviewSubmissionService) {
        this.reviewSubmissionService = reviewSubmissionService;
        this.reviewService = reviewService;
    }

    /**
     * Submits a Đánh_Giá_Sản_Phẩm for the Sản_Phẩm identified by {@code id}
     * on behalf of the authenticated Customer, rejecting the request if
     * the Customer has not purchased this product in a Đơn_Hàng with
     * Trạng_Thái_Đơn_Hàng = Đã_Giao (Requirement 9.1, 9.2).
     */
    @PostMapping(value = ApiPaths.Review.BY_PRODUCT_ID, consumes = "application/json")
    public ResponseEntity<ReviewResponse> submitReview(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        JwtPrincipal principal = currentCustomerPrincipal(authentication);

        ProductReview review = reviewService.submit(
                principal.accountId(), id, request.orderItemId(), request.rating(), request.comment());

        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.describe(review.getId()));
    }

    @PostMapping(value = ApiPaths.Review.BY_PRODUCT_ID, consumes = "multipart/form-data")
    public ResponseEntity<ReviewResponse> submitReviewWithImages(
            Authentication authentication, @PathVariable UUID id,
            @Valid @org.springframework.web.bind.annotation.RequestPart("review") CreateReviewRequest request,
            @org.springframework.web.bind.annotation.RequestPart(value = "images", required = false)
            java.util.List<org.springframework.web.multipart.MultipartFile> images) {
        var principal = currentCustomerPrincipal(authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                reviewSubmissionService.submit(principal.accountId(), id, request, images));
    }

    @GetMapping("/api/account/review-eligibility")
    public ResponseEntity<java.util.List<com.example.sport_be.dto.review.ReviewEligibilityResponse>> reviewEligibility(
            Authentication authentication, @RequestParam UUID productId) {
        return ResponseEntity.ok(reviewService.eligibleItems(currentCustomerPrincipal(authentication).accountId(), productId));
    }

    /**
     * Returns the non-hidden Đánh_Giá_Sản_Phẩm for the Sản_Phẩm identified
     * by {@code id}, together with their average rating, for display on
     * the Sản_Phẩm detail page (Requirement 9.3). Public: no
     * authentication required.
     */
    @GetMapping(ApiPaths.Review.BY_PRODUCT_ID)
    public ResponseEntity<ProductReviewListResponse> getReviews(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.getByProduct(id));
    }

    /**
     * Hides the Đánh_Giá_Sản_Phẩm identified by {@code id}, removing it
     * from the Sản_Phẩm detail page and its average rating (Requirement
     * 9.4). Restricted to {@code ROLE_ADMIN} via the {@code /api/admin/**}
     * matcher in {@code SecurityConfig}.
     */
    @DeleteMapping(ApiPaths.Review.ADMIN_BY_ID)
    public ResponseEntity<ReviewResponse> hideReview(@PathVariable UUID id) {
        ProductReview review = reviewService.hide(id);
        return ResponseEntity.ok(ReviewResponse.from(review));
    }

    /**
     * Returns a page of Đánh_Giá_Sản_Phẩm (including already-hidden ones)
     * for the Admin review management page, newest first (Requirement
     * 9.4), optionally filtered by a case-insensitive substring matched
     * against the reviewed Sản_Phẩm's name or the review's comment.
     * Restricted to {@code ROLE_ADMIN} via the {@code /api/admin/**}
     * matcher in {@code SecurityConfig}.
     */
    @GetMapping(ApiPaths.Review.ADMIN_LIST)
    public ResponseEntity<PageResponse<ReviewResponse>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(reviewService.getAllPaged(page, size, keyword));
    }

    private JwtPrincipal currentCustomerPrincipal(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        if (principal.role() != Role.CUSTOMER) {
            throw new ForbiddenException(CUSTOMER_ONLY_MESSAGE);
        }
        return principal;
    }
}
