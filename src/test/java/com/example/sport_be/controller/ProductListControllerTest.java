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
}
