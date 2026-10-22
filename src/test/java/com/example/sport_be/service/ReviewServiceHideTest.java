package com.example.sport_be.service;

import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductReview;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.ProductRepository;
import com.example.sport_be.repository.ProductReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ReviewService#hide} (admin hiding a Đánh_Giá_Sản_Phẩm
 * that violates content guidelines, Requirement 9.4).
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceHideTest {

    @Mock
    private ProductReviewRepository productReviewRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AccountRepository accountRepository;

    private ReviewService reviewService;

    private ProductReview review;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(productReviewRepository, orderItemRepository,
                productRepository, accountRepository);

        Product product = mock(Product.class);
        Account account = mock(Account.class);
        OrderItem orderItem = mock(OrderItem.class);
        review = new ProductReview(product, account, orderItem, 5);
    }

    @Test
    void hide_withVisibleReview_setsHiddenAndSaves() {
        UUID reviewId = UUID.randomUUID();
        when(productReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductReview result = reviewService.hide(reviewId);

        assertThat(result.isHidden()).isTrue();
        verify(productReviewRepository).save(review);
    }

    @Test
    void hide_withAlreadyHiddenReview_isIdempotentAndDoesNotSaveAgain() {
        review.setHidden(true);
        UUID reviewId = UUID.randomUUID();
        when(productReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        ProductReview result = reviewService.hide(reviewId);

        assertThat(result.isHidden()).isTrue();
        verify(productReviewRepository, never()).save(any());
    }

    @Test
    void hide_withUnknownReviewId_throwsResourceNotFoundException() {
        UUID reviewId = UUID.randomUUID();
        when(productReviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.hide(reviewId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productReviewRepository, never()).save(any());
    }
}
