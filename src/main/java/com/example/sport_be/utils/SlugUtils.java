package com.example.sport_be.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utility methods for generating URL-friendly slugs from (typically Vietnamese)
 * product and category names.
 *
 * <p>Used to populate fields such as {@code Category.slug}.
 */
public final class SlugUtils {

    private static final Pattern COMBINING_DIACRITICAL_MARKS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NON_ALPHANUMERIC_SEQUENCE = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-{2,}");

    private SlugUtils() {
        // static utility class, no instances
    }

    /**
     * Converts the given input string into a URL-friendly slug.
     *
     * <p>Vietnamese diacritics are stripped (e.g. {@code "Áo thể thao nam"} becomes
     * {@code "ao-the-thao-nam"}), the result is lowercased, any run of non
     * alphanumeric characters is collapsed into a single hyphen, and leading/trailing
     * hyphens are trimmed.
     *
     * @param input the raw name to slugify; must not be {@code null} or blank
     * @return the generated slug
     * @throws IllegalArgumentException if {@code input} is {@code null} or blank
     */
    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input must not be null or blank");
        }

        // Đ/đ are not decomposed by Unicode NFD normalization, so special-case them first.
        String withoutDBreve = input
                .replace('Đ', 'D')
                .replace('đ', 'd');

        String lowerCased = withoutDBreve.toLowerCase();

        String normalized = Normalizer.normalize(lowerCased, Normalizer.Form.NFD);
        String withoutDiacritics = COMBINING_DIACRITICAL_MARKS.matcher(normalized).replaceAll("");

        String hyphenated = NON_ALPHANUMERIC_SEQUENCE.matcher(withoutDiacritics).replaceAll("-");
        String collapsed = MULTIPLE_HYPHENS.matcher(hyphenated).replaceAll("-");

        return trimHyphens(collapsed);
    }

    private static String trimHyphens(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '-') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '-') {
            end--;
        }
        return value.substring(start, end);
    }
}
