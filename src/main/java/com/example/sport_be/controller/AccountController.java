package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.account.AccountResponse;
import com.example.sport_be.dto.account.UpdateProfileRequest;
import com.example.sport_be.entity.Account;
import com.example.sport_be.security.JwtPrincipal;
import com.example.sport_be.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints for the currently authenticated account to view and update
 * its own profile (Requirement 6.6). Available to any authenticated
 * role (Customer or Admin); access is gated by {@code SecurityConfig}
 * requiring authentication for any non-public route.
 */
@RestController
@RequestMapping(ApiPaths.Account.BASE)
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping(ApiPaths.Account.ME)
    public ResponseEntity<AccountResponse> getCurrentProfile(Authentication authentication) {
        Account account = accountService.getProfile(currentAccountId(authentication));
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PutMapping(ApiPaths.Account.ME)
    public ResponseEntity<AccountResponse> updateCurrentProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        Account account = accountService.updateProfile(currentAccountId(authentication), request);
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    private UUID currentAccountId(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return principal.accountId();
    }
}
