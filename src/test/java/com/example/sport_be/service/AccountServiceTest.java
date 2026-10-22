package com.example.sport_be.service;

import com.example.sport_be.dto.account.UpdateProfileRequest;
import com.example.sport_be.entity.Account;
import com.example.sport_be.enums.Role;
import com.example.sport_be.exception.BusinessRuleException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AccountService} (Requirement 6.6).
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository);
        account = new Account("user@example.com", "hashed-password", Role.CUSTOMER);
        account.setFullName("Old Name");
        account.setPhone("0900000000");
        account.setAddress("Old Address");
    }

    @Test
    void getProfile_withExistingAccountId_returnsAccount() {
        when(accountRepository.findById(any(UUID.class))).thenReturn(Optional.of(account));

        Account result = accountService.getProfile(UUID.randomUUID());

        assertThat(result).isEqualTo(account);
    }

    @Test
    void getProfile_withUnknownAccountId_throwsResourceNotFoundException() {
        when(accountRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getProfile(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_withValidData_updatesAndPersistsFields() {
        when(accountRepository.findById(any(UUID.class))).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "0911111111", "New Address");
        Account result = accountService.updateProfile(UUID.randomUUID(), request);

        assertThat(result.getFullName()).isEqualTo("New Name");
        assertThat(result.getPhone()).isEqualTo("0911111111");
        assertThat(result.getAddress()).isEqualTo("New Address");
        verify(accountRepository).save(account);
    }

    @Test
    void updateProfile_withUnknownAccountId_throwsResourceNotFoundException() {
        when(accountRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest("New Name", "0911111111", "New Address");

        assertThatThrownBy(() -> accountService.updateProfile(UUID.randomUUID(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setEnabled_withExistingAccountId_locksAccountAndSaves() {
        UUID accountId = UUID.randomUUID();
        UUID requesterAccountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.setEnabled(accountId, false, requesterAccountId);

        assertThat(result.isEnabled()).isFalse();
        verify(accountRepository).save(account);
    }

    @Test
    void setEnabled_withExistingAccountId_unlocksAccountAndSaves() {
        account.setEnabled(false);
        UUID accountId = UUID.randomUUID();
        UUID requesterAccountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.setEnabled(accountId, true, requesterAccountId);

        assertThat(result.isEnabled()).isTrue();
        verify(accountRepository).save(account);
    }

    @Test
    void setEnabled_withUnknownAccountId_throwsResourceNotFoundException() {
        UUID accountId = UUID.randomUUID();
        UUID requesterAccountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.setEnabled(accountId, false, requesterAccountId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setEnabled_whenLockingOwnAccount_throwsBusinessRuleExceptionWithoutSaving() {
        UUID accountId = UUID.randomUUID();

        assertThatThrownBy(() -> accountService.setEnabled(accountId, false, accountId))
                .isInstanceOf(BusinessRuleException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void setEnabled_whenUnlockingOwnAccount_isAllowed() {
        account.setEnabled(false);
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.setEnabled(accountId, true, accountId);

        assertThat(result.isEnabled()).isTrue();
    }
}
