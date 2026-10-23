package com.example.sport_be.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") @Size(max = 256) String currentPassword,
        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
        @Size(min = 6, max = 72, message = "Mật khẩu mới phải có từ 6 đến 72 ký tự") String newPassword,
        @NotBlank(message = "Vui lòng xác nhận mật khẩu mới") @Size(max = 72) String confirmPassword
) {
    @Override public String toString() { return "ChangePasswordRequest[REDACTED]"; }
}
