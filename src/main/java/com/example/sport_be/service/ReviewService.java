package com.example.sport_be.service;

import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.review.ProductReviewListResponse;
import com.example.sport_be.dto.review.ReviewResponse;
import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductReview;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.exception.BusinessRuleException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.ProductRepository;
import com.example.sport_be.repository.ProductReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;

/**
 * Handles submitting and retrieving Đánh_Giá_Sản_Phẩm (Product_Review)
 * records (Requirement 9).
 */
@Service
public class ReviewService {

    public static final String NOT_ELIGIBLE_ERROR_CODE = "NOT_ELIGIBLE_TO_REVIEW";
    public static final String ALREADY_REVIEWED_ERROR_CODE = "ORDER_ITEM_ALREADY_REVIEWED";
    private static final String ACCOUNT_NOT_FOUND_MESSAGE = "Không tìm thấy tài khoản.";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Không tìm thấy Sản_Phẩm.";
    private static final String ORDER_ITEM_NOT_FOUND_MESSAGE = "Không tìm thấy OrderItem.";
    private static final String REVIEW_NOT_FOUND_MESSAGE = "Không tìm thấy Đánh_Giá_Sản_Phẩm.";
    private static final String NOT_ELIGIBLE_MESSAGE =
            "Bạn chưa mua Sản_Phẩm này hoặc Đơn_Hàng liên quan chưa được giao, nên không đủ điều kiện đánh giá.";
    private static final String ALREADY_REVIEWED_MESSAGE = "Lần mua này đã được đánh giá trước đó.";

    private final ProductReviewRepository productReviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;

    public ReviewService(ProductReviewRepository productReviewRepository,
                          OrderItemRepository orderItemRepository,
                          ProductRepository productRepository,
                          AccountRepository accountRepository) {
        this.productReviewRepository = productReviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Submits a Đánh_Giá_Sản_Phẩm for {@code productId} on behalf of
     * {@code accountId}.
     *
     * <p>The Khách_Hàng is only eligible when there exists at least one
     * {@link OrderItem} referencing {@code productId} (via its
     * Biến_Thể_Sản_Phẩm) that belongs to one of the Khách_Hàng's own
     * Đơn_Hàng with {@link OrderStatus#DELIVERED} (Requirement 9.1, 9.2).
     * The review is linked to a specific, not-yet-reviewed eligible
     * {@code OrderItem}: either the one identified by {@code
     * requestedOrderItemId} (when provided), or the first eligible,
     * unreviewed one otherwise.
     *
     * @param accountId            the reviewing Khách_Hàng's Account id
     * @param productId            the Sản_Phẩm being reviewed
     * @param requestedOrderItemId optional specific purchased OrderItem to
     *                             attach the review to; when {@code null}
     *                             the first eligible, unreviewed order
     *                             item is used
     * @param rating               the review's rating (1-5)
     * @param comment              optional free-text comment
     * @return the saved {@link ProductReview}
     * @throws ResourceNotFoundException if the account or product does not exist,
     *                                    or {@code requestedOrderItemId} does not exist
     * @throws BusinessRuleException     if the account is not eligible to review this
     *                                    product, or the targeted order item was already reviewed
     */
    @Transactional
    public ProductReview submit(UUID accountId, UUID productId, UUID requestedOrderItemId, int rating, String comment) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(ACCOUNT_NOT_FOUND_MESSAGE));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        List<OrderItem> eligibleItems = orderItemRepository.findEligibleForReview(
                accountId, productId, OrderStatus.DELIVERED);

        OrderItem orderItem = selectOrderItem(eligibleItems, requestedOrderItemId);

        if (productReviewRepository.findByOrderItem_Id(orderItem.getId()).isPresent()) {
            throw new BusinessRuleException(ALREADY_REVIEWED_ERROR_CODE, ALREADY_REVIEWED_MESSAGE);
        }

        ProductReview review = new ProductReview(product, account, orderItem, rating);
        review.setComment(comment);

        return productReviewRepository.save(review);
    }

    /**
     * Returns the non-hidden Đánh_Giá_Sản_Phẩm for {@code productId},
     * together with their average rating and count, for display on the
     * Sản_Phẩm detail page (Requirement 9.3). Reviews hidden by an admin
     * ({@code hidden = true}, Requirement 9.4) are excluded from both the
     * list and the average.
     *
     * @param productId the Sản_Phẩm whose reviews are requested
     * @return the non-hidden reviews, their average rating (0 when there
     *         are none), and their count
     * @throws ResourceNotFoundException if the product does not exist
     */
    @Transactional(readOnly = true)
    public ProductReviewListResponse getByProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE);
        }

        List<ProductReview> visibleReviews = productReviewRepository.findByProduct_IdAndHiddenFalse(productId);

        double averageRating = visibleReviews.stream()
                .mapToInt(ProductReview::getRating)
                .average()
                .orElse(0.0);

        List<ReviewResponse> reviewResponses = visibleReviews.stream()
                .map(ReviewResponse::from)
                .toList();

        return new ProductReviewListResponse(reviewResponses, averageRating, reviewResponses.size());
    }

    /**
     * Returns every Đánh_Giá_Sản_Phẩm, newest first, regardless of
     * {@code hidden} status, for the Admin review management page
     * (Requirement 9.4).
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAll() {
        return productReviewRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * Returns a page of Đánh_Giá_Sản_Phẩm, newest first, regardless of
     * {@code hidden} status, for the Admin review management page
     * (Requirement 9.4). Optionally filtered by a case-insensitive
     * substring matched against either the reviewed Sản_Phẩm's name or
     * the review's comment.
     *
     * @param page    the 0-based page index
     * @param size    the page size
     * @param keyword optional case-insensitive substring to match against
     *                the product's name or the review's comment; ignored
     *                when blank
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAllPaged(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<ProductReview> spec = buildAdminListSpecification(keyword);

        Page<ProductReview> reviewPage = productReviewRepository.findAll(spec, pageable);

        return PageResponse.from(reviewPage, ReviewResponse::from);
    }

    private Specification<ProductReview> buildAdminListSpecification(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Specification.where(null);
        }

        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> {
            Predicate productNameMatch = cb.like(cb.lower(root.get("product").get("name")), pattern);
            Predicate commentMatch = cb.like(cb.lower(cb.coalesce(root.get("comment"), "")), pattern);
            return cb.or(productNameMatch, commentMatch);
        };
    }

    /**
     * Hides the Đánh_Giá_Sản_Phẩm identified by {@code reviewId} so it no
     * longer appears on the Sản_Phẩm detail page or counts toward its
     * average rating (Requirement 9.4). Idempotent: hiding an already
     * hidden review simply leaves it hidden without error.
     *
     * @param reviewId the Đánh_Giá_Sản_Phẩm to hide
     * @return the updated, hidden {@link ProductReview}
     * @throws ResourceNotFoundException if no review with this id exists
     */
    @Transactional
    public ProductReview hide(UUID reviewId) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(REVIEW_NOT_FOUND_MESSAGE));

        if (!review.isHidden()) {
            review.setHidden(true);
            review = productReviewRepository.save(review);
        }

        return review;
    }

    private OrderItem selectOrderItem(List<OrderItem> eligibleItems, UUID requestedOrderItemId) {
        if (eligibleItems.isEmpty()) {
            throw new BusinessRuleException(NOT_ELIGIBLE_ERROR_CODE, NOT_ELIGIBLE_MESSAGE);
        }

        if (requestedOrderItemId == null) {
            return eligibleItems.stream()
                    .filter(item -> productReviewRepository.findByOrderItem_Id(item.getId()).isEmpty())
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException(ALREADY_REVIEWED_ERROR_CODE, ALREADY_REVIEWED_MESSAGE));
        }

        return eligibleItems.stream()
                .filter(item -> item.getId().equals(requestedOrderItemId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException(NOT_ELIGIBLE_ERROR_CODE, NOT_ELIGIBLE_MESSAGE));
    }
}
