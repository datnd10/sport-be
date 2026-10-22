package com.example.sport_be.service;

import com.example.sport_be.dto.auth.LoginRequest;
import com.example.sport_be.dto.auth.LoginResponse;
import com.example.sport_be.dto.account.AccountResponse;
import com.example.sport_be.dto.auth.RegisterRequest;
import com.example.sport_be.entity.Account;
import com.example.sport_be.enums.Role;
import com.example.sport_be.exception.ConflictException;
import com.example.sport_be.exception.ForbiddenException;
import com.example.sport_be.exception.UnauthorizedException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles account registration and authentication (Requirement 6).
 */
@Service
public class AuthService {

    public static final String EMAIL_ALREADY_USED_ERROR_CODE = "EMAIL_ALREADY_USED";
    public static final String INVALID_CREDENTIALS_ERROR_CODE = "INVALID_CREDENTIALS";
    public static final String ACCOUNT_LOCKED_ERROR_CODE = "ACCOUNT_LOCKED";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Email hoặc mật khẩu không đúng.";
    private static final String ACCOUNT_LOCKED_MESSAGE = "Tài khoản của bạn đã bị khóa.";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new customer account.
     *
     * <p>Rejects the request with a {@link ConflictException} when the
     * email is already in use (Requirement 6.2); otherwise hashes the
     * password and persists a new {@link Account} with role
     * {@link Role#CUSTOMER} (Requirement 6.1).
     *
     * @throws ConflictException if an account with the given email already exists
     */
    @Transactional
    public Account register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new ConflictException(EMAIL_ALREADY_USED_ERROR_CODE, "Email đã được sử dụng.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        Account account = new Account(request.email(), passwordHash, Role.CUSTOMER);
        account.setFullName(request.fullName());
        account.setPhone(request.phone());
        account.setAddress(request.address());

        return accountRepository.save(account);
    }

    /**
     * Authenticates a user with email and password and issues a JWT access
     * token on success (Requirement 6.3).
     *
     * <p>When the email is not found OR the password does not match, the
     * same generic {@link UnauthorizedException} is thrown regardless of
     * which field was wrong, so the caller cannot infer whether the email
     * exists (Requirement 6.4).
     *
     * <p>Once the email/password combination is confirmed valid, the
     * account's {@code enabled} flag is checked: a Tài_Khoản that an
     * Admin has khóa (locked) is rejected with a {@link
     * ForbiddenException} even though the credentials are correct, so a
     * locked account cannot log back in until an Admin mở khóa
     * (unlocks) it.
     *
     * @throws UnauthorizedException if the email/password combination is invalid
     * @throws ForbiddenException    if the account is locked ({@code enabled = false})
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.email())
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw invalidCredentials();
        }

        if (!account.isEnabled()) {
            throw new ForbiddenException(ACCOUNT_LOCKED_ERROR_CODE, ACCOUNT_LOCKED_MESSAGE);
        }

        String token = jwtService.generateToken(account.getId(), account.getEmail(), account.getRole());
        return new LoginResponse(token, AccountResponse.from(account));
    }

    private UnauthorizedException invalidCredentials() {
        return new UnauthorizedException(INVALID_CREDENTIALS_ERROR_CODE, INVALID_CREDENTIALS_MESSAGE);
    }
}
