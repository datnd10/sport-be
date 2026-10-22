package com.example.sport_be.enums;

/**
 * Finite set of statuses an {@link Order} can be in, forming the order
 * status state machine:
 *
 * <pre>
 * PENDING -&gt; CONFIRMED -&gt; SHIPPING -&gt; DELIVERED
 * PENDING -&gt; CANCELLED
 * CONFIRMED -&gt; CANCELLED
 * </pre>
 *
 * <p>{@code DELIVERED} and {@code CANCELLED} are terminal statuses; no
 * transition is allowed out of them.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPING,
    DELIVERED,
    CANCELLED
}
