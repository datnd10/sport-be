package com.example.sport_be.repository;

import com.example.sport_be.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID>, JpaSpecificationExecutor<ProductReview> {

    List<ProductReview> findByProduct_IdAndHiddenFalse(UUID productId);

    Optional<ProductReview> findByOrderItem_Id(UUID orderItemId);

    List<ProductReview> findAllByOrderByCreatedAtDesc();
}
