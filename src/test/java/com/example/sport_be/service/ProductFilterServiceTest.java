package com.example.sport_be.service;

import com.example.sport_be.dto.product.ProductFilterCriteria;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.Product;
import com.example.sport_be.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ProductFilterServiceTest {
    @Test
    void filtersAverageRatingAndSortsPricesWithoutDroppingOtherCriteria() {
        ProductRepository products = mock(ProductRepository.class);
        ProductVariantRepository variants = mock(ProductVariantRepository.class);
        ProductImageRepository images = mock(ProductImageRepository.class);
        ProductReviewRepository reviews = mock(ProductReviewRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        ProductFilterService service = new ProductFilterService(products, variants, categories, images, reviews);
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(UUID.randomUUID());
        Product cheap = product(category, "Cheap", 100);
        Product costly = product(category, "Costly", 300);
        Product unrated = product(category, "Unrated", 50);
        when(products.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(cheap, costly, unrated));
        var ratingRows = List.of(new Object[]{cheap.getId(), 4.0, 2L}, new Object[]{costly.getId(), 4.5, 4L});
        when(reviews.summarizeRatings(any())).thenReturn(ratingRows);
        var criteria = new ProductFilterCriteria(null, null, null, null, Map.of(), 4.0, "price_desc");
        var result = service.search(criteria);
        assertThat(result.products()).extracting(p -> p.name()).containsExactly("Costly", "Cheap");
        assertThat(result.products().getFirst().averageRating()).isEqualTo(4.5);
        assertThat(result.products().getFirst().reviewCount()).isEqualTo(4);
        var rated = service.search(new ProductFilterCriteria(null, null, null, null, Map.of(), null, "rating_desc"));
        assertThat(rated.products()).extracting(p -> p.name()).containsExactly("Costly", "Cheap", "Unrated");
        var ascending = service.search(new ProductFilterCriteria(null, null, null, null, Map.of(), null, "price_asc"));
        assertThat(ascending.products()).extracting(p -> p.name()).containsExactly("Unrated", "Cheap", "Costly");
    }

    private Product product(Category category, String name, int price) {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(UUID.randomUUID());
        when(product.getCategory()).thenReturn(category);
        when(product.getName()).thenReturn(name);
        when(product.getBasePrice()).thenReturn(BigDecimal.valueOf(price));
        return product;
    }
}
