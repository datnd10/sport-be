package com.example.sport_be.dto.account;

import com.example.sport_be.entity.Account;
import com.example.sport_be.enums.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin-facing representation of a Tài_Khoản (Account), used by the
 * Admin Tài_Khoản management page: {@code GET /api/admin/accounts} and
 * {@code PUT /api/admin/accounts/{id}/enabled}. Deliberately omits
 * {@code passwordHash}.
 *
 * @param id        the account's unique identifier
 * @param email     the account's login email
 * @param fullName  the account owner's full name
 * @param phone     the account owner's contact phone number
 * @param address   the account owner's address
 * @param role      the account's role (CUSTOMER or ADMIN)
 * @param enabled   whether the account is currently allowed to log in
 * @param createdAt the instant the account was created
 */
public record AdminAccountResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String address,
        Role role,
        boolean enabled,
        Instant createdAt
) {

    public static AdminAccountResponse from(Account account) {
        return new AdminAccountResponse(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                account.getPhone(),
                account.getAddress(),
                account.getRole(),
                account.isEnabled(),
                account.getCreatedAt()
        );
    }
}
