package com.example.sport_be.dto.inventory;

import com.example.sport_be.entity.InventoryHistory;
import com.example.sport_be.enums.InventoryChangeReason;

import java.time.Instant;
import java.util.UUID;

/**
 * Representation of a single {@link InventoryHistory} entry, used by
 * {@code GET /api/admin/inventory/{variantId}/history} (Requirement 7.5).
 *
 * @param id           the history entry's unique identifier
 * @param variantId    the id of the variant this entry belongs to
 * @param changeAmount the signed change applied to {@code stockQuantity}
 * @param reason       why the change happened
 * @param changedAt    when the change happened
 */
public record InventoryHistoryResponse(
        UUID id,
        UUID variantId,
        int changeAmount,
        InventoryChangeReason reason,
        Instant changedAt
) {

    public static InventoryHistoryResponse from(InventoryHistory history) {
        return new InventoryHistoryResponse(
                history.getId(),
                history.getVariant().getId(),
                history.getChangeAmount(),
                history.getReason(),
                history.getChangedAt()
        );
    }
}
