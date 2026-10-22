package com.example.sport_be.dto.account;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for {@code PUT /api/account/me}.
 *
 * @param fullName the customer's full name
 * @param phone    the customer's contact phone number (optional)
 * @param address  the customer's address (optional)
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        String phone,

        String address
) {
}
