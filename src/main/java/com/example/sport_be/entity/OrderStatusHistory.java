package com.example.sport_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Records a single status transition of an {@link Order}, forming an
 * append-only audit trail of every status change.
 *
 * <p>{@code fromStatus} is nullable because the very first history entry
 * (order creation) has no previous status.
 */
@Entity
@Table(name = "order_status_histories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "from_status")
    private String fromStatus;

    @Column(name = "to_status", nullable = false)
    private String toStatus;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant changedAt;

    public OrderStatusHistory(Order order, String fromStatus, String toStatus) {
        this.order = order;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }
}
