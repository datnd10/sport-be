package com.example.sport_be.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility methods for formatting {@link BigDecimal} monetary amounts as
 * Vietnamese Dong (VND) display strings.
 */
public final class CurrencyUtils {

    private static final Locale VIETNAMESE_LOCALE = new Locale("vi", "VN");
    private static final String VND_SUFFIX = " \u20AB";

    private CurrencyUtils() {
        // static utility class, no instances
    }

    /**
     * Formats the given amount as a Vietnamese Dong display string, e.g.
     * {@code formatVnd(new BigDecimal("1250000"))} returns {@code "1.250.000 ₫"}.
     *
     * <p>VND has no minor/decimal unit in this project's domain (prices are
     * whole-number amounts), so the amount is rounded to 0 decimal places
     * (half-up) before formatting.
     *
     * @param amount the amount to format; must not be {@code null}
     * @return the formatted VND display string, including the "₫" suffix
     * @throws IllegalArgumentException if {@code amount} is {@code null}
     */
    public static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount must not be null");
        }

        BigDecimal wholeAmount = amount.setScale(0, RoundingMode.HALF_UP);

        NumberFormat numberFormat = NumberFormat.getInstance(VIETNAMESE_LOCALE);
        numberFormat.setGroupingUsed(true);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);

        String grouped = numberFormat.format(wholeAmount);
        return grouped + VND_SUFFIX;
    }
}
