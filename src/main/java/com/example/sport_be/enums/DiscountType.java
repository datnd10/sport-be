package com.example.sport_be.enums;

/**
 * How a {@link DiscountCode#getDiscountValue()} is applied to an order's
 * total amount.
 */
public enum DiscountType {
    /** {@code discountValue} is a percentage (0-100) off the order total. */
    PERCENTAGE,
    /** {@code discountValue} is a fixed monetary amount off the order total. */
    FIXED_AMOUNT
}
