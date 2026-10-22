package com.example.sport_be.dto.product;

import com.example.sport_be.dto.category.CategoryResponse;

import java.util.List;

/**
 * Result of {@code ProductFilterService.search}: the matching products,
 * plus a list of popular Danh_Mục suggestions that is only populated when
 * {@code products} is empty, so Khách_Hàng gets a "no results" message
 * together with suggestions (Requirement 2.4).
 *
 * @param products           the products matching every applied filter
 *                           condition (Requirement 2.3, 2.6)
 * @param suggestedCategories popular categories to suggest when no keyword
 *                           search result was found; empty whenever
 *                           {@code products} is non-empty (Requirement 2.4)
 */
public record ProductSearchResult(
        List<ProductResponse> products,
        List<CategoryResponse> suggestedCategories
) {

    public static ProductSearchResult withProducts(List<ProductResponse> products) {
        return new ProductSearchResult(products, List.of());
    }

    public static ProductSearchResult noResults(List<CategoryResponse> suggestedCategories) {
        return new ProductSearchResult(List.of(), suggestedCategories);
    }
}
