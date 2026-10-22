package com.example.sport_be.service;

import com.example.sport_be.dto.auth.LoginRequest;
import com.example.sport_be.dto.auth.LoginResponse;
import com.example.sport_be.entity.Account;
import com.example.sport_be.enums.Role;
import com.example.sport_be.exception.ForbiddenException;
import com.example.sport_be.exception.UnauthorizedException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService#login} (Requirement 6.3, 6.4), including
 * the Tài_Khoản lock check that rejects login for a Tài_Khoản an Admin has
 * khóa (locked, {@code enabled = false}).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private Account account;

    @BeforeEach
    void setUp() {
        authService = new AuthService(accountRepository, passwordEncoder, jwtService);
        account = new Account("user@example.com", "hashed-password", Role.CUSTOMER);
    }

    @Test
    void login_withValidCredentialsAndEnabledAccount_returnsTokenAndProfile() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("signed-jwt-token");

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("signed-jwt-token");
        assertThat(response.account().email()).isEqualTo("user@example.com");
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorizedException() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");
        when(accountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_withWrongPassword_throwsUnauthorizedException() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_withLockedAccountAndValidCredentials_throwsForbiddenExceptionWithoutIssuingToken() {
        account.setEnabled(false);
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        when(accountRepository.findByEmail("user@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ForbiddenException.class);

        verify(jwtService, never()).generateToken(any(), any(), any());
    }
}
