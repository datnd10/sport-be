package com.example.sport_be.controller;

import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductReview;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.PaymentMethod;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.CategoryRepository;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.OrderRepository;
import com.example.sport_be.repository.ProductRepository;
import com.example.sport_be.repository.ProductReviewRepository;
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
 * Integration tests for {@code GET /api/admin/reviews}
 * (ApiPaths.Review.ADMIN_LIST), covering pagination and keyword search
 * (by product name or comment) for the Admin Đánh_Giá_Sản_Phẩm management
 * page (Requirement 9.4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminReviewListControllerTest {

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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductReviewRepository productReviewRepository;

    private String adminToken;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);

        Account admin = new Account("review-admin-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.ADMIN);
        admin = accountRepository.save(admin);
        adminToken = jwtService.generateToken(admin.getId(), admin.getEmail(), admin.getRole());

        Account customer = new Account("review-customer-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.CUSTOMER);
        customer = accountRepository.save(customer);

        Category category = categoryRepository.save(new Category("Giày " + suffix, "giay-" + suffix));

        Product yonexShoe = productRepository.save(new Product(category, "Giày Yonex Power " + suffix, BigDecimal.valueOf(800_000)));
        ProductVariant yonexVariant = productVariantRepository.save(
                new ProductVariant(yonexShoe, "SKU-RY-" + suffix, "size=40", BigDecimal.valueOf(800_000), 10));

        Product mizunoShoe = productRepository.save(new Product(category, "Giày Mizuno Wave " + suffix, BigDecimal.valueOf(900_000)));
        ProductVariant mizunoVariant = productVariantRepository.save(
                new ProductVariant(mizunoShoe, "SKU-RM-" + suffix, "size=41", BigDecimal.valueOf(900_000), 8));

        Order order = orderRepository.save(new Order(customer, PaymentMethod.COD, BigDecimal.valueOf(1_700_000),
                BigDecimal.valueOf(1_700_000), "Nguyen Van A", "0900000000", "123 Le Loi"));

        OrderItem yonexItem = orderItemRepository.save(
                new OrderItem(order, yonexVariant, yonexShoe.getName(), yonexVariant.getPrice(), 1));
        OrderItem mizunoItem = orderItemRepository.save(
                new OrderItem(order, mizunoVariant, mizunoShoe.getName(), mizunoVariant.getPrice(), 1));

        ProductReview yonexReview = new ProductReview(yonexShoe, customer, yonexItem, 5);
        yonexReview.setComment("Rất hài lòng với sản phẩm " + suffix);
        productReviewRepository.save(yonexReview);

        ProductReview mizunoReview = new ProductReview(mizunoShoe, customer, mizunoItem, 2);
        mizunoReview.setComment("Chất lượng tệ " + suffix);
        productReviewRepository.save(mizunoReview);
    }

    @Test
    void getAllReviews_withDefaultPaging_returnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/admin/reviews")
                        .param("keyword", suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content.length()", is(2)));
    }

    @Test
    void getAllReviews_withSmallPageSize_paginatesResults() throws Exception {
        mockMvc.perform(get("/api/admin/reviews")
                        .param("keyword", suffix)
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void getAllReviews_withKeywordMatchingProductName_returnsOnlyMatchingReview() throws Exception {
        mockMvc.perform(get("/api/admin/reviews")
                        .param("keyword", "yonex")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.productName =~ /.*[Mm]izuno.*/)]").doesNotExist());
    }

    @Test
    void getAllReviews_withKeywordMatchingComment_returnsOnlyMatchingReview() throws Exception {
        mockMvc.perform(get("/api/admin/reviews")
                        .param("keyword", "tệ " + suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].comment", is("Chất lượng tệ " + suffix)));
    }

    @Test
    void getAllReviews_withoutToken_isRejectedAsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isUnauthorized());
    }
}
