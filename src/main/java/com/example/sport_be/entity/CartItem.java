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

import java.util.UUID;

/**
 * Represents a single line item within a {@link Cart}, referencing the
 * {@link ProductVariant} being purchased and the requested quantity.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private int quantity;

    public CartItem(Cart cart, ProductVariant variant, int quantity) {
        this.cart = cart;
        this.variant = variant;
        this.quantity = quantity;
    }
}
