package com.example.sport_be.dto.discount;

import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.enums.DiscountType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response payload for {@code GET /api/discount-codes/validate}: reports
 * that a Mã_Giảm_Giá is currently eligible to be applied to an order
 * (Requirement 8.3, 8.4).
 *
 * <p>Returned only when validation succeeds; ineligible codes (not found,
 * inactive, expired, or usage limit reached) result in an error response
 * via {@code GlobalExceptionHandler} instead of a {@code valid=false}
 * body, so callers get a clear, specific reason.
 *
 * @param id            the discount code's unique identifier
 * @param code          the discount code's unique identifier string
 * @param discountType  how {@code discountValue} is applied to an order's total
 * @param discountValue the discount amount/percentage
 * @param valid         whether the code is currently eligible to be applied; always {@code true} when returned
 */
public record DiscountValidationResponse(
        UUID id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        boolean valid
) {

    public static DiscountValidationResponse from(DiscountCode discountCode) {
        return new DiscountValidationResponse(
                discountCode.getId(),
                discountCode.getCode(),
                discountCode.getDiscountType(),
                discountCode.getDiscountValue(),
                true
        );
    }
}
