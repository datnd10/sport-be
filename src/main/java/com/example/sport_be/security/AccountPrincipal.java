package com.example.sport_be.security;

import com.example.sport_be.entity.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security {@link UserDetails} adapter around {@link Account}.
 *
 * <p>Exposes the account's role as a {@code ROLE_*} authority (e.g.
 * {@code ROLE_ADMIN}) as expected by {@code hasRole(...)} security
 * expressions.
 */
public class AccountPrincipal implements UserDetails {

    private final UUID accountId;
    private final String email;
    private final String passwordHash;
    private final List<GrantedAuthority> authorities;

    public AccountPrincipal(Account account) {
        this.accountId = account.getId();
        this.email = account.getEmail();
        this.passwordHash = account.getPasswordHash();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));
    }

    public UUID getAccountId() {
        return accountId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
