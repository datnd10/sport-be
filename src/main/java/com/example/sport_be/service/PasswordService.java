package com.example.sport_be.service;

import com.example.sport_be.dto.account.ChangePasswordRequest;
import com.example.sport_be.exception.FieldError;
import com.example.sport_be.exception.ForbiddenException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class PasswordService {
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    public PasswordService(AccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    @Transactional
    public void change(UUID accountId, ChangePasswordRequest request) {
        var account = accounts.findForPasswordChange(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if (!account.isEnabled()) throw new ForbiddenException("Tài khoản đã bị khóa.");
        if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || !encoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw invalid("currentPassword", "Mật khẩu hiện tại không đúng.");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw invalid("confirmPassword", "Xác nhận mật khẩu mới không khớp.");
        }
        if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalid("newPassword", "Mật khẩu mới vượt quá 72 byte. Hãy sử dụng mật khẩu ngắn hơn.");
        }
        if (encoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw invalid("newPassword", "Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        account.setPasswordHash(encoder.encode(request.newPassword()));
        accounts.save(account);
    }

    private ValidationException invalid(String field, String message) {
        return new ValidationException(message, List.of(new FieldError(field, message)));
    }
}
