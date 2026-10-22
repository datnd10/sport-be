package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.account.AdminAccountResponse;
import com.example.sport_be.dto.account.SetAccountEnabledRequest;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.entity.Account;
import com.example.sport_be.security.JwtPrincipal;
import com.example.sport_be.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Endpoints for the Admin Tài_Khoản (Account) management page: listing
 * and searching every Tài_Khoản (Khách_Hàng and Quản_Trị_Viên), and
 * khóa/mở khóa (locking/unlocking) one to control login access.
 *
 * <p>Admin only has view and lock/unlock capabilities here — no account
 * deletion and no role changes. Routes under {@code /api/admin/**}
 * require {@code ROLE_ADMIN}, per {@code SecurityConfig}.
 */
@RestController
public class AdminAccountController {

    private final AccountService accountService;

    public AdminAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Returns a page of every Tài_Khoản, newest first, optionally
     * filtered by a case-insensitive substring matched against
     * {@code email} or {@code fullName}.
     */
    @GetMapping(ApiPaths.AdminAccount.ADMIN_LIST)
    public ResponseEntity<PageResponse<AdminAccountResponse>> listAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(accountService.findAllPaged(page, size, keyword));
    }

    /**
     * Khóa (locks) or mở khóa (unlocks) the Tài_Khoản identified by
     * {@code id}. The currently logged-in Admin cannot lock their own
     * account.
     */
    @PutMapping(ApiPaths.AdminAccount.ADMIN_SET_ENABLED)
    public ResponseEntity<AdminAccountResponse> setAccountEnabled(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody SetAccountEnabledRequest request) {
        UUID requesterAccountId = currentAccountId(authentication);
        Account account = accountService.setEnabled(id, request.enabled(), requesterAccountId);
        return ResponseEntity.ok(AdminAccountResponse.from(account));
    }

    private UUID currentAccountId(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return principal.accountId();
    }
}
