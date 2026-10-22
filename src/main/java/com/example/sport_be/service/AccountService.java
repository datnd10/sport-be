package com.example.sport_be.service;

import com.example.sport_be.dto.account.AdminAccountResponse;
import com.example.sport_be.dto.account.UpdateProfileRequest;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.entity.Account;
import com.example.sport_be.exception.BusinessRuleException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.AccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles reading and updating the current account's own profile
 * (Requirement 6.6), and the Admin Tài_Khoản (Account) management page:
 * listing/searching every Tài_Khoản and khóa/mở khóa (locking/unlocking)
 * one to control login access.
 */
@Service
public class AccountService {

    public static final String ACCOUNT_NOT_FOUND_ERROR_CODE = "ACCOUNT_NOT_FOUND";
    public static final String CANNOT_LOCK_OWN_ACCOUNT_ERROR_CODE = "CANNOT_LOCK_OWN_ACCOUNT";
    private static final String CANNOT_LOCK_OWN_ACCOUNT_MESSAGE = "Không thể tự khóa tài khoản đang đăng nhập.";

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Retrieves the account identified by {@code accountId}.
     *
     * @throws ResourceNotFoundException if no account exists with that id
     */
    @Transactional(readOnly = true)
    public Account getProfile(UUID accountId) {
        return findAccountOrThrow(accountId);
    }

    /**
     * Updates the profile fields (full name, phone, address) of the
     * account identified by {@code accountId} and persists the change
     * (Requirement 6.6).
     *
     * @throws ResourceNotFoundException if no account exists with that id
     */
    @Transactional
    public Account updateProfile(UUID accountId, UpdateProfileRequest request) {
        Account account = findAccountOrThrow(accountId);
        account.setFullName(request.fullName());
        account.setPhone(request.phone());
        account.setAddress(request.address());
        return accountRepository.save(account);
    }

    private Account findAccountOrThrow(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ACCOUNT_NOT_FOUND_ERROR_CODE, "Không tìm thấy tài khoản."));
    }

    /**
     * Returns a page of every Tài_Khoản (Khách_Hàng and Quản_Trị_Viên),
     * newest first, for the Admin Tài_Khoản management page, optionally
     * filtered by a case-insensitive substring matched against
     * {@code email} or {@code fullName}.
     *
     * @param page    the 0-based page index
     * @param size    the page size
     * @param keyword optional case-insensitive substring to match against
     *                {@code email} or {@code fullName}; ignored when blank
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminAccountResponse> findAllPaged(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Account> spec = buildAdminListSpecification(keyword);

        Page<Account> accountPage = accountRepository.findAll(spec, pageable);

        return PageResponse.from(accountPage, AdminAccountResponse::from);
    }

    private Specification<Account> buildAdminListSpecification(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Specification.where(null);
        }

        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> {
            Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
            Predicate fullNameMatch = cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), pattern);
            return cb.or(emailMatch, fullNameMatch);
        };
    }

    /**
     * Khóa (locks, {@code enabled = false}) or mở khóa (unlocks, {@code
     * enabled = true}) the Tài_Khoản identified by {@code accountId}, so
     * it can no longer (or can again) log in via {@code AuthService.login}.
     *
     * <p>An Admin cannot lock the Tài_Khoản they are currently logged in
     * as, to avoid locking themselves out; {@code requesterAccountId} is
     * the id of the Admin account making this request.
     *
     * @param accountId          the Tài_Khoản to lock/unlock
     * @param enabled             the desired login-allowed state
     * @param requesterAccountId the id of the Admin account performing this action
     * @return the updated {@link Account}
     * @throws ResourceNotFoundException if no account exists with that id
     * @throws BusinessRuleException     if the Admin is trying to lock their own account
     */
    @Transactional
    public Account setEnabled(UUID accountId, boolean enabled, UUID requesterAccountId) {
        if (!enabled && accountId.equals(requesterAccountId)) {
            throw new BusinessRuleException(CANNOT_LOCK_OWN_ACCOUNT_ERROR_CODE, CANNOT_LOCK_OWN_ACCOUNT_MESSAGE);
        }

        Account account = findAccountOrThrow(accountId);
        account.setEnabled(enabled);
        return accountRepository.save(account);
    }
}
