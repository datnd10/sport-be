package com.example.sport_be.dto.product;

import com.example.sport_be.dto.category.CategoryResponse;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductImage;
import com.example.sport_be.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Full public-facing representation of a Sản_Phẩm (Product) detail page:
 * name, description, price, Danh_Mục (Category), all available
 * Biến_Thể_Sản_Phẩm (ProductVariant) with stock/out-of-stock status, and
 * Hình_Ảnh_Sản_Phẩm (ProductImage) list (Requirement 2.5).
 *
 * @param id          the product's unique identifier
 * @param category    the category this product belongs to
 * @param name        the product's name
 * @param description the product's description
 * @param basePrice   the product's base price
 * @param active      whether the product is currently visible (soft-delete flag)
 * @param variants    the product's variants, including stock/out-of-stock status
 * @param images      the product's images, ordered by {@code displayOrder}
 */
public record ProductDetailResponse(
        UUID id,
        CategoryResponse category,
        String name,
        String description,
        BigDecimal basePrice,
        boolean active,
        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images
) {

    public static ProductDetailResponse from(Product product, List<ProductVariant> variants, List<ProductImage> images) {
        return new ProductDetailResponse(
                product.getId(),
                CategoryResponse.from(product.getCategory()),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.isActive(),
                variants.stream().map(ProductVariantResponse::from).toList(),
                images.stream().map(ProductImageResponse::from).toList()
        );
    }
}