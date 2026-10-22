package com.example.sport_be.dto.product;

import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductImage;
import com.example.sport_be.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Representation of a Sản_Phẩm (Product) together with its
 * Biến_Thể_Sản_Phẩm (ProductVariant) list and Hình_Ảnh_Sản_Phẩm
 * (ProductImage) list, returned by
 * {@code ProductService.create}/{@code update} (Requirement 1.2, 1.4, 1.6)
 * and by the public product list/search endpoints (Requirement 2.1, 2.3)
 * so storefront cards can render a thumbnail without a second request
 * per product.
 *
 * @param id          the product's unique identifier
 * @param categoryId  the id of the category this product belongs to
 * @param name        the product's name
 * @param description the product's description
 * @param basePrice   the product's base price
 * @param active      whether the product is currently visible (soft-delete flag)
 * @param variants    the product's variants
 * @param images      the product's images, ordered by {@code displayOrder}
 */
public record ProductResponse(
        UUID id,
        UUID categoryId,
        String name,
        String description,
        BigDecimal basePrice,
        boolean active,
        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images
) {

    public static ProductResponse from(Product product, List<ProductVariant> variants, List<ProductImage> images) {
        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.isActive(),
                variants.stream().map(ProductVariantResponse::from).toList(),
                images.stream().map(ProductImageResponse::from).toList()
        );
    }
}
