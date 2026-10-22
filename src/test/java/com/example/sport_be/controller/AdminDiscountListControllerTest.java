package com.example.sport_be.controller;

import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.enums.DiscountType;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.DiscountCodeRepository;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code GET /api/admin/discount-codes}
 * (ApiPaths.Discount.ADMIN_LIST), covering pagination and keyword search
 * for the Admin Mã_Giảm_Giá management page (Requirement 8.1, 8.3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDiscountListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DiscountCodeRepository discountCodeRepository;

    private String adminToken;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);

        Account admin = new Account("discount-admin-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.ADMIN);
        admin = accountRepository.save(admin);
        adminToken = jwtService.generateToken(admin.getId(), admin.getEmail(), admin.getRole());

        Instant now = Instant.now();

        DiscountCode summer1 = new DiscountCode("SUMMER10-" + suffix, DiscountType.PERCENTAGE, BigDecimal.TEN,
                now.minus(1, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS), 100);
        discountCodeRepository.save(summer1);

        DiscountCode summer2 = new DiscountCode("SUMMER20-" + suffix, DiscountType.PERCENTAGE, BigDecimal.valueOf(20),
                now.minus(2, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS), 50);
        discountCodeRepository.save(summer2);

        DiscountCode winter = new DiscountCode("WINTER5-" + suffix, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(5000),
                now.minus(3, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS), 20);
        discountCodeRepository.save(winter);
    }

    @Test
    void getAllDiscountCodes_withDefaultPaging_returnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/admin/discount-codes")
                        .param("keyword", suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.content.length()", is(3)));
    }

    @Test
    void getAllDiscountCodes_withSmallPageSize_paginatesResults() throws Exception {
        mockMvc.perform(get("/api/admin/discount-codes")
                        .param("keyword", suffix)
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content.length()", is(2)));
    }

    @Test
    void getAllDiscountCodes_withKeyword_returnsOnlyMatchingCodesCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/admin/discount-codes")
                        .param("keyword", "summer")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.code =~ /.*WINTER.*/i)]").doesNotExist());
    }

    @Test
    void getAllDiscountCodes_withLowercaseKeywordMatchingUppercaseCode_isCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/admin/discount-codes")
                        .param("keyword", ("winter5-" + suffix).toLowerCase())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].code", is("WINTER5-" + suffix)));
    }

    @Test
    void getAllDiscountCodes_withoutToken_isRejectedAsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/discount-codes"))
                .andExpect(status().isUnauthorized());
    }
}
