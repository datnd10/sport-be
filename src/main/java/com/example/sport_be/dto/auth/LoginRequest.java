package com.example.sport_be.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for {@code POST /api/auth/login}.
 *
 * @param email    the account's login email
 * @param password the plaintext password to verify against the stored hash
 */
public record LoginRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password
) {
}
