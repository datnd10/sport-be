package com.example.sport_be.enums;

/**
 * Reason recorded on an {@link InventoryHistory} entry for why a
 * {@link ProductVariant}'s stock quantity changed.
 */
public enum InventoryChangeReason {
    /** Stock was decreased because an order was placed. */
    ORDER_PLACED,
    /** Stock was restored because an order was cancelled. */
    ORDER_CANCELLED,
    /** Stock was changed directly by an admin. */
    MANUAL_ADJUSTMENT
}
