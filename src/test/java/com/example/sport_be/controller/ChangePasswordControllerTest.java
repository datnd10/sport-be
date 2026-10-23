package com.example.sport_be.controller;

import com.example.sport_be.entity.Account;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtService jwt;
    Account account;
    String token;
    @BeforeEach void fixture() {
        account = accounts.save(new Account(UUID.randomUUID() + "@example.com", encoder.encode("Current@123"), Role.CUSTOMER));
        token = jwt.generateToken(account.getId(), account.getEmail(), account.getRole());
    }
    String request(String old, String password, String confirm) throws Exception {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(java.util.Map.of("currentPassword", old, "newPassword", password, "confirmPassword", confirm));
    }
    @Test void changesOnlyOwnPasswordAndNewPasswordCanLogin() throws Exception {
        var other = accounts.save(new Account(UUID.randomUUID() + "@example.com", encoder.encode("Other@123"), Role.CUSTOMER));
        mvc.perform(put("/api/account/me/password").header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(request("Current@123", "New@456", "New@456")))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        var stored = accounts.findById(account.getId()).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("New@456");
        assertThat(encoder.matches("New@456", stored.getPasswordHash())).isTrue();
        assertThat(encoder.matches("Current@123", stored.getPasswordHash())).isFalse();
        assertThat(encoder.matches("Other@123", accounts.findById(other.getId()).orElseThrow().getPasswordHash())).isTrue();
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"" + account.getEmail() + "\",\"password\":\"Current@123\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"" + account.getEmail() + "\",\"password\":\"New@456\"}"))
                .andExpect(status().isOk());
    }
    @Test void rejectsWrongCurrentPasswordAndKeepsExistingHash() throws Exception {
        String before = account.getPasswordHash();
        mvc.perform(put("/api/account/me/password").header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(request("Wrong@123", "New@456", "New@456")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"));
        assertThat(accounts.findById(account.getId()).orElseThrow().getPasswordHash()).isEqualTo(before);
    }
    @Test void rejectsMismatchReuseShortAndOversizedUnicodePasswords() throws Exception {
        for (var values : new String[][] {
                {"New@456", "Different@456"}, {"Current@123", "Current@123"}, {"123", "123"}, {"ắ".repeat(30), "ắ".repeat(30)} }) {
            mvc.perform(put("/api/account/me/password").header("Authorization", "Bearer " + token).contentType("application/json")
                            .content(request("Current@123", values[0], values[1])))
                    .andExpect(status().isBadRequest());
        }
        assertThat(encoder.matches("Current@123", accounts.findById(account.getId()).orElseThrow().getPasswordHash())).isTrue();
    }
    @Test void requiresAuthentication() throws Exception {
        mvc.perform(put("/api/account/me/password").contentType("application/json").content(request("Current@123", "New@456", "New@456")))
                .andExpect(status().isUnauthorized());
    }
    @Test void disabledAccountCannotChangePassword() throws Exception {
        account.setEnabled(false); accounts.save(account);
        mvc.perform(put("/api/account/me/password").header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(request("Current@123", "New@456", "New@456")))
                .andExpect(status().isForbidden());
    }
}
