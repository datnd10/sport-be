package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.account.AccountResponse;
import com.example.sport_be.dto.auth.LoginRequest;
import com.example.sport_be.dto.auth.LoginResponse;
import com.example.sport_be.dto.auth.RegisterRequest;
import com.example.sport_be.entity.Account;
import com.example.sport_be.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for account registration and authentication
 * (Requirement 6).
 */
@RestController
@RequestMapping(ApiPaths.Auth.BASE)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(ApiPaths.Auth.REGISTER)
    public ResponseEntity<AccountResponse> register(@Valid @RequestBody RegisterRequest request) {
        Account account = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @PostMapping(ApiPaths.Auth.LOGIN)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
