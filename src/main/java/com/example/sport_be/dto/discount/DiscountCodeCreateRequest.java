package com.example.sport_be.dto.discount;

import com.example.sport_be.enums.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Request payload for {@code POST /api/admin/discount-codes}: creates a
 * new Mã_Giảm_Giá (Discount_Code) (Requirement 8.1, 8.2).
 *
 * @param code          the discount code's unique identifier string; required and must not be blank (Requirement 8.2)
 * @param discountType  how {@code discountValue} is applied to an order's total; required
 * @param discountValue the discount amount/percentage; required and must be positive
 * @param validFrom     the instant from which the code becomes active; required
 * @param validTo       the instant after which the code is no longer valid; required
 * @param maxUsage      the maximum number of times the code may be applied; required and must be at least 1
 */
public record DiscountCodeCreateRequest(
        @NotBlank(message = "Mã_Giảm_Giá không được để trống")
        String code,

        @NotNull(message = "Loại giảm giá không được để trống")
        DiscountType discountType,

        @NotNull(message = "Giá trị giảm giá không được để trống")
        @Positive(message = "Giá trị giảm giá phải lớn hơn 0")
        BigDecimal discountValue,

        @NotNull(message = "Thời điểm bắt đầu hiệu lực không được để trống")
        Instant validFrom,

        @NotNull(message = "Thời điểm hết hiệu lực không được để trống")
        Instant validTo,

        @NotNull(message = "Số lần sử dụng tối đa không được để trống")
        @Min(value = 1, message = "Số lần sử dụng tối đa phải lớn hơn hoặc bằng 1")
        Integer maxUsage
) {
}
