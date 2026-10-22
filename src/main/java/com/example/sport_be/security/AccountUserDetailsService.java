package com.example.sport_be.security;

import com.example.sport_be.entity.Account;
import com.example.sport_be.repository.AccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads an {@link Account} by email (used as the username) for Spring
 * Security's authentication machinery. Backs both the JWT-based
 * authentication filter and the future {@code AuthService.login} flow
 * (task 5.4), which will authenticate via {@link
 * org.springframework.security.authentication.AuthenticationManager}.
 */
@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public AccountUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email));
        return new AccountPrincipal(account);
    }
}
