package com.example.sport_be.repository;
import com.example.sport_be.entity.ProductReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ProductReviewImageRepository extends JpaRepository<ProductReviewImage, UUID> {}
