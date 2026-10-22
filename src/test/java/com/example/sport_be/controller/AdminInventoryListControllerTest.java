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
 * Integration tests for {@code GET /api/admin/inventory}, covering
 * pagination and keyword search (by SKU or related Sản_Phẩm name) for
 * the Admin Tồn_Kho management list (Requirement 7.3, 7.4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminInventoryListControllerTest {

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
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString();

        Account admin = new Account("admin-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.ADMIN);
        admin = accountRepository.save(admin);
        adminToken = jwtService.generateToken(admin.getId(), admin.getEmail(), admin.getRole());

        Category category = categoryRepository.save(new Category("Vợt cầu lông " + suffix, "vot-cau-long-" + suffix));

        Product yonex = productRepository.save(new Product(category, "Vợt Yonex Astrox " + suffix, BigDecimal.valueOf(500_000)));
        productVariantRepository.save(new ProductVariant(yonex, "SKU-Y1-" + suffix, "color=red", BigDecimal.valueOf(500_000), 10));

        Product mizuno = productRepository.save(new Product(category, "Giày Mizuno Wave " + suffix, BigDecimal.valueOf(1_200_000)));
        productVariantRepository.save(new ProductVariant(mizuno, "SKU-M1-" + suffix, "size=42", BigDecimal.valueOf(1_200_000), 3));
    }

    @Test
    void listInventory_withDefaultPaging_returnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/admin/inventory")
                        .param("keyword", suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content.length()", is(2)));
    }

    @Test
    void listInventory_withSmallPageSize_paginatesResults() throws Exception {
        mockMvc.perform(get("/api/admin/inventory")
                        .param("keyword", suffix)
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void listInventory_withKeywordMatchingSku_returnsOnlyMatchingVariant() throws Exception {
        mockMvc.perform(get("/api/admin/inventory")
                        .param("keyword", "SKU-Y1-" + suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].sku", is("SKU-Y1-" + suffix)));
    }

    @Test
    void listInventory_withKeywordMatchingProductName_returnsOnlyMatchingVariant() throws Exception {
        mockMvc.perform(get("/api/admin/inventory")
                        .param("keyword", "Mizuno Wave " + suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].productName", is("Giày Mizuno Wave " + suffix)));
    }

    @Test
    void listInventory_withoutToken_isRejectedAsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/inventory"))
                .andExpect(status().isUnauthorized());
    }
}
