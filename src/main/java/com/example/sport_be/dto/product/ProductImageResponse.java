package com.example.sport_be.dto.product;

import com.example.sport_be.entity.ProductImage;

import java.util.UUID;

/**
 * Representation of a Hình_Ảnh_Sản_Phẩm (ProductImage) returned as part
 * of a {@link ProductDetailResponse} (Requirement 2.5).
 *
 * @param id           the image's unique identifier
 * @param filePath     the relative path to the stored image file
 * @param displayOrder the order in which the image should be rendered (ascending)
 */
public record ProductImageResponse(
        UUID id,
        String filePath,
        int displayOrder
) {

    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getFilePath(),
                image.getDisplayOrder()
        );
    }
}
