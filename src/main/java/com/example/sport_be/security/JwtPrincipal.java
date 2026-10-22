package com.example.sport_be.security;

import com.example.sport_be.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

/**
 * Lightweight authenticated principal built directly from a validated JWT's
 * claims, without hitting the database on every request. Used by
 * {@link JwtAuthenticationFilter} to populate the security context.
 *
 * @param accountId the account id extracted from the token subject
 * @param email     the account's email, extracted from the token claims
 * @param role      the account's role, extracted from the token claims
 */
public record JwtPrincipal(UUID accountId, String email, Role role) {

    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
