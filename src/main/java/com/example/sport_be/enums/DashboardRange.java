package com.example.sport_be.enums;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Selectable time range for the admin Dashboard summary (Requirement
 * 10.2): hôm nay (today), 7 ngày qua (last 7 days) or tháng này (this
 * month).
 *
 * <p>Each range resolves to a half-open {@code [from, to)} instant
 * interval, computed against the server's default time zone, so that
 * widening/narrowing a range only ever grows/shrinks the interval
 * (Property 33).
 */
public enum DashboardRange {
    TODAY,
    LAST_7_DAYS,
    THIS_MONTH;

    /**
     * Parses the {@code range} query parameter value into a
     * {@link DashboardRange}. Accepted values: {@code today}, {@code 7days},
     * {@code month} (case-insensitive).
     *
     * @throws IllegalArgumentException if {@code value} does not match any known range key
     */
    public static DashboardRange fromKey(String value) {
        if (value == null) {
            throw new IllegalArgumentException("range là bắt buộc.");
        }
        return switch (value.trim().toLowerCase()) {
            case "today" -> TODAY;
            case "7days" -> LAST_7_DAYS;
            case "month" -> THIS_MONTH;
            default -> throw new IllegalArgumentException("Giá trị range không hợp lệ: " + value);
        };
    }

    /**
     * Resolves this range to a half-open {@code [from, to)} instant
     * interval, anchored to "now" in the given zone.
     */
    public Interval resolve(ZoneId zone, Instant now) {
        ZonedDateTime nowZoned = now.atZone(zone);
        LocalDate today = nowZoned.toLocalDate();

        return switch (this) {
            case TODAY -> {
                Instant from = today.atStartOfDay(zone).toInstant();
                Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();
                yield new Interval(from, to);
            }
            case LAST_7_DAYS -> {
                Instant to = today.plusDays(1).atStartOfDay(zone).toInstant();
                Instant from = today.minusDays(6).atStartOfDay(zone).toInstant();
                yield new Interval(from, to);
            }
            case THIS_MONTH -> {
                YearMonth currentMonth = YearMonth.from(today);
                Instant from = currentMonth.atDay(1).atStartOfDay(zone).toInstant();
                Instant to = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
                yield new Interval(from, to);
            }
        };
    }

    /** A half-open {@code [from, to)} instant interval. */
    public record Interval(Instant from, Instant to) {
    }
}
