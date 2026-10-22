package com.example.sport_be.dto.account;

import com.example.sport_be.entity.Account;
import com.example.sport_be.enums.Role;

import java.util.UUID;

/**
 * Public-facing representation of an {@link Account}, deliberately
 * omitting {@code passwordHash}.
 *
 * @param id       the account's unique identifier
 * @param email    the account's login email
 * @param fullName the account owner's full name
 * @param phone    the account owner's contact phone number
 * @param address  the account owner's address
 * @param role     the account's role (CUSTOMER or ADMIN)
 */
public record AccountResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String address,
        Role role
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getEmail(),
                account.getFullName(),
                account.getPhone(),
                account.getAddress(),
                account.getRole()
        );
    }
}
