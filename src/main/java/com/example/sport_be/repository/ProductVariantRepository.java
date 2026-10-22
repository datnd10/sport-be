package com.example.sport_be.repository;

import com.example.sport_be.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID>, JpaSpecificationExecutor<ProductVariant> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    List<ProductVariant> findByProduct_Id(UUID productId);

    /**
     * Returns only the currently active (not soft-deleted) Biến_Thể_Sản_Phẩm
     * for a Sản_Phẩm, used everywhere a variant list is surfaced to a
     * caller (public storefront or Admin), so a soft-deleted variant
     * never reappears in a response.
     */
    List<ProductVariant> findByProduct_IdAndActiveTrue(UUID productId);
}
