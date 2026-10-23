package com.example.sport_be.repository;

import com.example.sport_be.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProduct_IdInOrderByDisplayOrderAscIdAsc(List<UUID> productIds);

    List<ProductImage> findByProduct_IdOrderByDisplayOrderAsc(UUID productId);
}
