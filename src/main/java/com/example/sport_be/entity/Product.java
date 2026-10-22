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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a product belonging to a {@link Category}.
 *
 * <p>A product is "soft-deleted" by setting {@code active} to {@code false}
 * instead of being removed from the database, so that {@code OrderItem}
 * snapshots referencing this product remain valid even after it is hidden
 * from customer-facing listings.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public Product(Category category, String name, BigDecimal basePrice) {
        this.category = category;
        this.name = name;
        this.basePrice = basePrice;
    }
}
