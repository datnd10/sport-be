package com.example.sport_be.dto.account;

/**
 * Request payload for {@code PUT /api/admin/accounts/{id}/enabled}, used
 * by an Admin to khóa (lock, {@code enabled = false}) or mở khóa
 * (unlock, {@code enabled = true}) a Tài_Khoản.
 *
 * @param enabled the desired login-allowed state of the account
 */
public record SetAccountEnabledRequest(
        boolean enabled
) {
}
