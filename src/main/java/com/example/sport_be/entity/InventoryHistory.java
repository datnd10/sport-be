package com.example.sport_be.entity;

import com.example.sport_be.enums.InventoryChangeReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Records a single change to a {@link ProductVariant}'s stock quantity,
 * forming an append-only audit trail.
 *
 * <p>{@code changeAmount} is the signed delta applied to
 * {@code stockQuantity} (positive for increases such as restocks or order
 * cancellations, negative for decreases such as placed orders).
 * {@code InventoryService} is the only component permitted to create
 * these records, one per {@code stockQuantity} change (Requirement 7.5).
 */
@Entity
@Table(name = "inventory_histories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryHistory {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "change_amount", nullable = false)
    private int changeAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryChangeReason reason;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant changedAt;

    public InventoryHistory(ProductVariant variant, int changeAmount, InventoryChangeReason reason) {
        this.variant = variant;
        this.changeAmount = changeAmount;
        this.reason = reason;
    }
}
