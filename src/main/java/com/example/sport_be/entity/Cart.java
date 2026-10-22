package com.example.sport_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents the server-side shopping cart belonging to exactly one
 * {@link Account}.
 *
 * <p>Guest (not logged in) carts have no corresponding {@code Cart} record;
 * the frontend holds temporary cart state and calls {@code /api/cart/merge}
 * on login to merge it into the account's server-side cart.
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    public Cart(Account account) {
        this.account = account;
    }
}
