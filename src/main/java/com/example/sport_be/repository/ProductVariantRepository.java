package com.example.sport_be.repository;

import com.example.sport_be.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

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

    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product p WHERE v.active = true AND p.active = true " +
            "AND (v.stockQuantity = 0 OR v.stockQuantity < v.lowStockThreshold) ORDER BY v.stockQuantity ASC, v.sku ASC")
    List<ProductVariant> findStockAlerts(Pageable pageable);

    @Query("SELECT COUNT(v) FROM ProductVariant v WHERE v.active = true AND v.product.active = true " +
            "AND v.stockQuantity > 0 AND v.stockQuantity < v.lowStockThreshold")
    long countLowStockActiveVariants();

    @Query("SELECT COUNT(v) FROM ProductVariant v WHERE v.active = true AND v.product.active = true AND v.stockQuantity = 0")
    long countOutOfStockActiveVariants();
}
