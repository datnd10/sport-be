package com.example.sport_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a customer's review of a {@link Product}.
 *
 * <p>{@code orderItem} references the exact {@link OrderItem} that was
 * purchased and delivered, used to verify review eligibility (Requirement
 * 9.1, 9.2) and to prevent a customer from reviewing the same purchase more
 * than once. {@code hidden} defaults to {@code false} and is set to
 * {@code true} by an admin to hide a review that violates guidelines
 * (Requirement 9.4).
 */
@Entity
@Table(name = "product_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = "order_item_id"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductReview {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private boolean hidden = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public ProductReview(Product product, Account account, OrderItem orderItem, int rating) {
        this.product = product;
        this.account = account;
        this.orderItem = orderItem;
        this.rating = rating;
    }
}
