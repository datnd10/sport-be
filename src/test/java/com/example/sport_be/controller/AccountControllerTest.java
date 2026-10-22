package com.example.sport_be.controller;

import com.example.sport_be.dto.account.UpdateProfileRequest;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code GET /api/account/me} and
 * {@code PUT /api/account/me} (Requirement 6.6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerTest {

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

    private Account account;
    private String token;

    @BeforeEach
    void setUp() {
        String uniqueEmail = "profile-test-" + UUID.randomUUID() + "@example.com";
        account = new Account(uniqueEmail, passwordEncoder.encode("password123"), Role.CUSTOMER);
        account.setFullName("Nguyen Van A");
        account.setPhone("0900000000");
        account.setAddress("123 Le Loi, Q1");
        account = accountRepository.save(account);

        token = jwtService.generateToken(account.getId(), account.getEmail(), account.getRole());
    }

    @Test
    void getCurrentProfile_withValidToken_returnsProfileWithoutPasswordHash() throws Exception {
        mockMvc.perform(get("/api/account/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(account.getEmail())))
                .andExpect(jsonPath("$.fullName", is("Nguyen Van A")))
                .andExpect(jsonPath("$.phone", is("0900000000")))
                .andExpect(jsonPath("$.address", is("123 Le Loi, Q1")))
                .andExpect(jsonPath("$.role", is("CUSTOMER")))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getCurrentProfile_withoutToken_isRejectedAsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/account/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCurrentProfile_withValidData_persistsAndReturnsUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Nguyen Van B", "0911111111", "456 Tran Hung Dao, Q5");

        mockMvc.perform(put("/api/account/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Nguyen Van B")))
                .andExpect(jsonPath("$.phone", is("0911111111")))
                .andExpect(jsonPath("$.address", is("456 Tran Hung Dao, Q5")));

        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("Nguyen Van B");
        assertThat(updated.getPhone()).isEqualTo("0911111111");
        assertThat(updated.getAddress()).isEqualTo("456 Tran Hung Dao, Q5");
    }

    @Test
    void updateCurrentProfile_withoutToken_isRejectedAsUnauthorized() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Someone", "0900000001", "Somewhere");

        mockMvc.perform(put("/api/account/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCurrentProfile_withBlankFullName_isRejectedAsBadRequest() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("   ", "0900000001", "Somewhere");

        mockMvc.perform(put("/api/account/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
