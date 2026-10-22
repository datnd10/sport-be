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
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a single purchased line item within an {@link Order}.
 *
 * <p>{@code productNameSnapshot} and {@code unitPriceSnapshot} capture the
 * product name and unit price at the time of purchase, so order history
 * remains accurate even if the referenced {@link ProductVariant}'s parent
 * {@code Product} is later edited or soft-deleted.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false)
    private int quantity;

    public OrderItem(Order order, ProductVariant variant, String productNameSnapshot,
                      BigDecimal unitPriceSnapshot, int quantity) {
        this.order = order;
        this.variant = variant;
        this.productNameSnapshot = productNameSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
    }
}
