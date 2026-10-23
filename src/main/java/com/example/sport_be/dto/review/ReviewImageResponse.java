package com.example.sport_be.dto.review;
import com.example.sport_be.entity.ProductReviewImage;
import java.util.UUID;
public record ReviewImageResponse(UUID id, String filePath, int displayOrder) {
    public static ReviewImageResponse from(ProductReviewImage image) {
        return new ReviewImageResponse(image.getId(), image.getFilePath(), image.getDisplayOrder());
    }
}
