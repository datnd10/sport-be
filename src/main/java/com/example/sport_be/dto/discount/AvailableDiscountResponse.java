package com.example.sport_be.dto.discount;

import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.enums.DiscountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Customer-facing offer details; excludes internal usage counters and administration fields. */
public record AvailableDiscountResponse(UUID id, String code, DiscountType discountType,
        BigDecimal discountValue, Instant validTo) {
    public static AvailableDiscountResponse from(DiscountCode code) {
        return new AvailableDiscountResponse(code.getId(), code.getCode(), code.getDiscountType(),
                code.getDiscountValue(), code.getValidTo());
    }
}
