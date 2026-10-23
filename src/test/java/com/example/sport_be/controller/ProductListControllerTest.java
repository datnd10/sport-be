package com.example.sport_be.controller;

import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.repository.CategoryRepository;
import com.example.sport_be.repository.ProductRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code GET /api/products} (Requirement 2.1, 2.2),
 * covering the public listing endpoint used both for the homepage's
 * "Sản_Phẩm nổi bật" section and for category/keyword/price filtering.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private Category racketCategory;
    private Category shoeCategory;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        racketCategory = categoryRepository.save(new Category("Vợt cầu lông " + suffix, "vot-cau-long-" + suffix));
        shoeCategory = categoryRepository.save(new Category("Giày thể thao " + suffix, "giay-the-thao-" + suffix));

        Product racket = productRepository.save(new Product(racketCategory, "Vợt Yonex " + suffix, BigDecimal.valueOf(500_000)));
        productVariantRepository.save(new ProductVariant(racket, "SKU-RACKET-" + suffix, "color=red", BigDecimal.valueOf(500_000), 10));

        Product shoe = productRepository.save(new Product(shoeCategory, "Giày Mizuno " + suffix, BigDecimal.valueOf(1_200_000)));
        productVariantRepository.save(new ProductVariant(shoe, "SKU-SHOE-" + suffix, "size=42", BigDecimal.valueOf(1_200_000), 5));
    }

    @Test
    void listProducts_withoutFilters_isPublicAndIncludesSeededActiveProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products[?(@.categoryId=='" + racketCategory.getId() + "')]", hasSize(1)))
                .andExpect(jsonPath("$.products[?(@.categoryId=='" + shoeCategory.getId() + "')]", hasSize(1)));
    }

    @Test
    void listProducts_filteredByCategoryId_returnsOnlyProductsInThatCategory() throws Exception {
        mockMvc.perform(get("/api/products").param("categoryId", racketCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.products[0].categoryId", org.hamcrest.Matchers.is(racketCategory.getId().toString())));
    }

    @Test
    void listProducts_filteredByPriceRange_excludesProductsOutsideRange() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("categoryId", racketCategory.getId().toString())
                        .param("minPrice", "1000000")
                        .param("maxPrice", "2000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(0)));

        mockMvc.perform(get("/api/products")
                        .param("categoryId", shoeCategory.getId().toString())
                        .param("minPrice", "1000000")
                        .param("maxPrice", "2000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(1)));
    }

    @Test
    void listProducts_withUnmatchedKeyword_returnsNoResultsWithSuggestedCategories() throws Exception {
        mockMvc.perform(get("/api/products").param("keyword", "khong-ton-tai-xyz-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products", hasSize(0)));
    }

    @Test
    void listCategories_isPublicAndReturnsAllCategories() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())));
    }
    @Autowired private com.example.sport_be.repository.ProductReviewRepository reviewRepository;
    @Autowired private com.example.sport_be.repository.AccountRepository accountRepository;
    @Autowired private com.example.sport_be.repository.OrderRepository orderRepository;
    @Autowired private com.example.sport_be.repository.OrderItemRepository orderItemRepository;

    @Test
    void ratingFilterUsesVisibleAverageAndCombinesWithExistingFilters() throws Exception {
        var product = productRepository.findAll().stream()
                .filter(p -> p.getCategory().getId().equals(racketCategory.getId())).findFirst().orElseThrow();
        var variant = productVariantRepository.findByProduct_IdAndActiveTrue(product.getId()).getFirst();
        var account = accountRepository.save(new com.example.sport_be.entity.Account(
                UUID.randomUUID() + "@test.com", "hash", com.example.sport_be.enums.Role.CUSTOMER));
        var order = orderRepository.save(new com.example.sport_be.entity.Order(account,
                com.example.sport_be.enums.PaymentMethod.COD, BigDecimal.TEN, BigDecimal.TEN, "Test", "0123456789", "Test address"));
        for (int rating : new int[]{4, 5, 1}) {
            var item = orderItemRepository.save(new com.example.sport_be.entity.OrderItem(order, variant, "Test", BigDecimal.TEN, 1));
            var review = new com.example.sport_be.entity.ProductReview(product, account, item, rating);
            review.setHidden(rating == 1);
            reviewRepository.save(review);
        }
        mockMvc.perform(get("/api/products").param("categoryId", racketCategory.getId().toString())
                        .param("keyword", "Yonex").param("minPrice", "400000").param("maxPrice", "600000")
                        .param("minRating", "4").param("sort", "rating_desc").param("page", "0").param("size", "12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.products[0].averageRating").value(4.5))
                .andExpect(jsonPath("$.products[0].reviewCount").value(2));
        mockMvc.perform(get("/api/products").param("categoryId", racketCategory.getId().toString()).param("minRating", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.products", hasSize(0)));
    }

    @Test
    void rejectsInvalidFilterValues() throws Exception {
        mockMvc.perform(get("/api/products").param("minRating", "6")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("minPrice", "100").param("maxPrice", "50")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("sort", "invalid")).andExpect(status().isBadRequest());
    }

    @Test
    void paginatesAfterFilteringAndSortingAcrossAllProducts() throws Exception {
        for (int i = 1; i <= 25; i++) {
            var product = productRepository.save(new Product(racketCategory, "Page product " + i, BigDecimal.valueOf(i * 100)));
            productVariantRepository.save(new ProductVariant(product, "PAGE-" + UUID.randomUUID(), "", BigDecimal.valueOf(i * 100), 10));
        }
        mockMvc.perform(get("/api/products").param("categoryId", racketCategory.getId().toString())
                        .param("keyword", "Page product").param("sort", "price_asc").param("page", "1").param("size", "12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.products", hasSize(12)))
                .andExpect(jsonPath("$.products[0].basePrice").value(1300))
                .andExpect(jsonPath("$.totalElements").value(25)).andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(1));
        mockMvc.perform(get("/api/products").param("categoryId", racketCategory.getId().toString())
                        .param("keyword", "Page product").param("sort", "price_desc").param("page", "2").param("size", "12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.products", hasSize(1)))
                .andExpect(jsonPath("$.products[0].basePrice").value(100));
        mockMvc.perform(get("/api/products").param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products").param("size", "101")).andExpect(status().isBadRequest());
    }

}
