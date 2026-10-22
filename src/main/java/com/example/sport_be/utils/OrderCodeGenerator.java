package com.example.sport_be.utils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates human-readable order codes for display to customers, independent
 * of the internal UUID primary key used by {@code Order.id}.
 */
public final class OrderCodeGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SUFFIX_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SUFFIX_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private OrderCodeGenerator() {
        // static utility class, no instances
    }

    /**
     * Generates an order code in the format {@code ORD-yyyyMMdd-XXXXXX}, where
     * {@code yyyyMMdd} is the current date and {@code XXXXXX} is a 6-character
     * random uppercase alphanumeric suffix drawn from a {@link SecureRandom}.
     *
     * <p><strong>Uniqueness is not guaranteed.</strong> This method combines the
     * current date with a random suffix, which makes collisions practically
     * negligible for this shop's order volume, but it is not enforced by any
     * database constraint. Callers must not rely on the generated code as a
     * primary key or unique identifier — the actual {@code Order.id} (UUID)
     * remains the source of truth for uniqueness.
     *
     * @return a newly generated, human-readable order code
     */
    public static String generate() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String suffix = randomSuffix();
        return "ORD-" + datePart + "-" + suffix;
    }

    private static String randomSuffix() {
        StringBuilder builder = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            int index = RANDOM.nextInt(SUFFIX_ALPHABET.length());
            builder.append(SUFFIX_ALPHABET.charAt(index));
        }
        return builder.toString();
    }
}
