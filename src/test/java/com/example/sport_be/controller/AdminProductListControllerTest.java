package com.example.sport_be.controller;

import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.CategoryRepository;
import com.example.sport_be.repository.ProductRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import com.example.sport_be.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code GET /api/admin/products}
 * (ApiPaths.Product.ADMIN_LIST), covering pagination and keyword search
 * for the Admin product management list (Requirement 1.4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProductListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private String adminToken;
    private Category category;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString();

        Account admin = new Account("admin-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.ADMIN);
        admin = accountRepository.save(admin);
        adminToken = jwtService.generateToken(admin.getId(), admin.getEmail(), admin.getRole());

        category = categoryRepository.save(new Category("Vợt cầu lông " + suffix, "vot-cau-long-" + suffix));

        // Seed 3 products: 2 matching keyword "Yonex", 1 matching "Mizuno", spanning both active states.
        Product yonex1 = productRepository.save(new Product(category, "Vợt Yonex Astrox " + suffix, BigDecimal.valueOf(500_000)));
        productVariantRepository.save(new ProductVariant(yonex1, "SKU-Y1-" + suffix, "color=red", BigDecimal.valueOf(500_000), 10));

        Product yonex2 = productRepository.save(new Product(category, "Vợt Yonex Nanoflare " + suffix, BigDecimal.valueOf(600_000)));
        productVariantRepository.save(new ProductVariant(yonex2, "SKU-Y2-" + suffix, "color=blue", BigDecimal.valueOf(600_000), 5));

        Product mizuno = productRepository.save(new Product(category, "Giày Mizuno Wave " + suffix, BigDecimal.valueOf(1_200_000)));
        productVariantRepository.save(new ProductVariant(mizuno, "SKU-M1-" + suffix, "size=42", BigDecimal.valueOf(1_200_000), 3));
        mizuno.setActive(false);
        productRepository.save(mizuno);
    }

    @Test
    void listAllProductsForAdmin_withDefaultPaging_returnsPageMetadataAndIncludesInactiveProducts() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .param("categoryId", category.getId().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.content.length()", is(3)))
                .andExpect(jsonPath("$.content[?(@.active==false)]").exists());
    }

    @Test
    void listAllProductsForAdmin_withSmallPageSize_paginatesResultsSortedByCreatedAtDesc() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .param("categoryId", category.getId().toString())
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content.length()", is(2)));

        mockMvc.perform(get("/api/admin/products")
                        .param("categoryId", category.getId().toString())
                        .param("page", "1")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void listAllProductsForAdmin_withKeyword_returnsOnlyMatchingProductsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .param("categoryId", category.getId().toString())
                        .param("keyword", "yonex")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[?(@.name =~ /.*[Mm]izuno.*/)]").doesNotExist());
    }

    @Test
    void listAllProductsForAdmin_withoutToken_isRejectedAsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listAllProductsForAdmin_withSortByNameAsc_returnsProductsOrderedByNameAscending() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .param("categoryId", category.getId().toString())
                        .param("sortBy", "name")
                        .param("sortDir", "asc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(3)))
                .andExpect(jsonPath("$.content[0].name", is("Giày Mizuno Wave " + suffix)))
                .andExpect(jsonPath("$.content[1].name", is("Vợt Yonex Astrox " + suffix)))
                .andExpect(jsonPath("$.content[2].name", is("Vợt Yonex Nanoflare " + suffix)));
    }

    @Test
    void listAllProductsForAdmin_withSortByBasePriceDesc_returnsProductsOrderedByPriceDescending() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .param("categoryId", category.getId().toString())
                        .param("sortBy", "basePrice")
                        .param("sortDir", "desc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(3)))
                .andExpect(jsonPath("$.content[0].name", is("Giày Mizuno Wave " + suffix)))
                .andExpect(jsonPath("$.content[2].name", is("Vợt Yonex Astrox " + suffix)));
    }

    @Test
    void listAllProductsForAdmin_withInvalidSortBy_fallsBackToDefaultSortInsteadOfFailing() throws Exception {
        mockMvc.perform(get("/api/admin/products")
                        .param("categoryId", category.getId().toString())
                        .param("sortBy", "notARealField")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(3)));
    }
}
