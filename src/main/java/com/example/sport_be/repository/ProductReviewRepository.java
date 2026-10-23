package com.example.sport_be.repository;

import com.example.sport_be.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID>, JpaSpecificationExecutor<ProductReview> {

    @Query("select r.product.id, avg(r.rating), count(r) from ProductReview r where r.hidden = false and r.product.id in :ids group by r.product.id")
    List<Object[]> summarizeRatings(@Param("ids") List<UUID> ids);

    List<ProductReview> findByProduct_IdAndHiddenFalse(UUID productId);

    Optional<ProductReview> findByOrderItem_Id(UUID orderItemId);

    List<ProductReview> findAllByOrderByCreatedAtDesc();
}
