package com.example.sport_be.dto.discount;

import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.enums.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Public-facing representation of a {@link DiscountCode}, used by
 * {@code POST /api/admin/discount-codes} (Requirement 8.1).
 *
 * @param id            the discount code's unique identifier
 * @param code          the discount code's unique identifier string
 * @param discountType  how {@code discountValue} is applied to an order's total
 * @param discountValue the discount amount/percentage
 * @param validFrom     the instant from which the code becomes active
 * @param validTo       the instant after which the code is no longer valid
 * @param maxUsage      the maximum number of times the code may be applied
 * @param usageCount    the number of times the code has been applied so far
 * @param active        whether the code is currently enabled
 */
public record DiscountCodeResponse(
        UUID id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        Instant validFrom,
        Instant validTo,
        int maxUsage,
        int usageCount,
        boolean active
) {

    public static DiscountCodeResponse from(DiscountCode discountCode) {
        return new DiscountCodeResponse(
                discountCode.getId(),
                discountCode.getCode(),
                discountCode.getDiscountType(),
                discountCode.getDiscountValue(),
                discountCode.getValidFrom(),
                discountCode.getValidTo(),
                discountCode.getMaxUsage(),
                discountCode.getUsageCount(),
                discountCode.isActive()
        );
    }
}
