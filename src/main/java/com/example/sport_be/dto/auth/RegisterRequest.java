package com.example.sport_be.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for {@code POST /api/auth/register}.
 *
 * @param email    the account's unique login email
 * @param password the plaintext password to be hashed before storage
 * @param fullName the customer's full name
 * @param phone    the customer's contact phone number (optional)
 * @param address  the customer's address (optional)
 */
public record RegisterRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        String phone,

        String address
) {
}
