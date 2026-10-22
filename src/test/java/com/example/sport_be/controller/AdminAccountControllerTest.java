package com.example.sport_be.controller;

import com.example.sport_be.entity.Account;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Admin Tài_Khoản management endpoints:
 * {@code GET /api/admin/accounts} (pagination/search) and
 * {@code PUT /api/admin/accounts/{id}/enabled} (lock/unlock, including the
 * self-lock guard and that a locked account is rejected at login).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private Account admin;
    private String adminToken;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);

        admin = new Account("acc-admin-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.ADMIN);
        admin.setFullName("Quan Tri Vien " + suffix);
        admin = accountRepository.save(admin);
        adminToken = jwtService.generateToken(admin.getId(), admin.getEmail(), admin.getRole());

        Account customer1 = new Account("acc-nguyen-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.CUSTOMER);
        customer1.setFullName("Nguyen Van A " + suffix);
        accountRepository.save(customer1);

        Account customer2 = new Account("acc-tran-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.CUSTOMER);
        customer2.setFullName("Tran Thi B " + suffix);
        accountRepository.save(customer2);
    }

    @Test
    void listAllAccounts_withDefaultPaging_returnsPageMetadata() throws Exception {
        mockMvc.perform(get("/api/admin/accounts")
                        .param("keyword", suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.content.length()", is(3)))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
    }

    @Test
    void listAllAccounts_withKeywordMatchingFullName_returnsOnlyMatchingAccount() throws Exception {
        mockMvc.perform(get("/api/admin/accounts")
                        .param("keyword", "Nguyen Van A " + suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].fullName", is("Nguyen Van A " + suffix)));
    }

    @Test
    void listAllAccounts_withKeywordMatchingEmail_returnsOnlyMatchingAccount() throws Exception {
        mockMvc.perform(get("/api/admin/accounts")
                        .param("keyword", "acc-tran-" + suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].email", is("acc-tran-" + suffix + "@example.com")));
    }

    @Test
    void listAllAccounts_withoutToken_isRejectedAsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setAccountEnabled_lockingAnotherAccount_disablesLoginForThatAccount() throws Exception {
        Account target = new Account("acc-lock-target-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.CUSTOMER);
        target = accountRepository.save(target);

        mockMvc.perform(put("/api/admin/accounts/" + target.getId() + "/enabled")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(false)));

        Account updated = accountRepository.findById(target.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(updated.isEnabled());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", target.getEmail(),
                                "password", "password123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void setAccountEnabled_lockingOwnAccount_isRejectedAsUnprocessable() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/" + admin.getId() + "/enabled")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isUnprocessableEntity());

        Account unchanged = accountRepository.findById(admin.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(unchanged.isEnabled());
    }

    @Test
    void setAccountEnabled_withUnknownAccountId_returnsNotFound() throws Exception {
        mockMvc.perform(put("/api/admin/accounts/" + UUID.randomUUID() + "/enabled")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isNotFound());
    }
}
