package com.example.sport_be.dto.product;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Combined search/filter criteria accepted by
 * {@code ProductFilterService.search} (Requirement 2.3, 2.6).
 *
 * <p>Every field is optional; any field left {@code null} (or an empty
 * {@code variantAttributes} map) is simply not applied as a filter
 * condition, so passing all-null criteria returns every active product.
 *
 * @param keyword           case-insensitive, partial-match keyword searched
 *                          against the product's name OR description
 *                          (Requirement 2.3)
 * @param categoryId        restricts results to products belonging to this
 *                           Danh_Mục (Requirement 2.6)
 * @param minPrice           restricts results to products with at least one
 *                           Biến_Thể_Sản_Phẩm priced at or above this value
 *                           (Requirement 2.6)
 * @param maxPrice           restricts results to products with at least one
 *                           Biến_Thể_Sản_Phẩm priced at or below this value
 *                           (Requirement 2.6)
 * @param variantAttributes  restricts results to products having at least
 *                           one Biến_Thể_Sản_Phẩm whose {@code attributes}
 *                           text contains every given key/value pair
 *                           (case-insensitive, e.g. {@code size=42}),
 *                           (Requirement 2.6)
 */
public record ProductFilterCriteria(
        String keyword,
        UUID categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Map<String, String> variantAttributes,
        Double minRating,
        String sort
) {

    public ProductFilterCriteria(String keyword, UUID categoryId, BigDecimal minPrice,
                                 BigDecimal maxPrice, Map<String, String> variantAttributes) {
        this(keyword, categoryId, minPrice, maxPrice, variantAttributes, null, null);
    }

    public static ProductFilterCriteria empty() {
        return new ProductFilterCriteria(null, null, null, null, Map.of());
    }

    public Map<String, String> variantAttributes() {
        return variantAttributes == null ? Map.of() : variantAttributes;
    }

    public boolean hasKeyword() {
        return keyword != null && !keyword.isBlank();
    }
}
