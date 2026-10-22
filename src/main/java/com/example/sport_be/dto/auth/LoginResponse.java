package com.example.sport_be.dto.auth;

import com.example.sport_be.dto.account.AccountResponse;

/**
 * Response payload for {@code POST /api/auth/login}, carrying the issued
 * JWT access token alongside the authenticated account's public profile
 * (Requirement 6.3).
 *
 * @param token   the signed JWT access token
 * @param account the authenticated account's public-facing profile
 */
public record LoginResponse(
        String token,
        AccountResponse account
) {
}