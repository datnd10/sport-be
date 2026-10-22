package com.example.sport_be.entity;

import com.example.sport_be.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a discount code that customers can apply at checkout to
 * reduce an {@link Order}'s total amount.
 *
 * <p>The {@code code} column has a {@code UNIQUE} constraint at the
 * database level, combined with service-layer checks in
 * {@code DiscountService} to give clear error messages before violating
 * the constraint (Requirement 8.2).
 *
 * <p>{@code usageCount} starts at 0 and is incremented when the code is
 * successfully applied to an order; validity is determined by
 * {@code active}, the {@code [validFrom, validTo]} window, and
 * {@code usageCount < maxUsage}.
 */
@Entity
@Table(name = "discount_codes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscountCode {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(name = "max_usage", nullable = false)
    private int maxUsage;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;

    @Column(nullable = false)
    private boolean active = true;

    public DiscountCode(String code, DiscountType discountType, BigDecimal discountValue,
                         Instant validFrom, Instant validTo, int maxUsage) {
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.maxUsage = maxUsage;
    }
}
