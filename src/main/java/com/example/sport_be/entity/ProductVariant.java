package com.example.sport_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents a purchasable variant of a {@link Product} (e.g. a specific
 * size/color combination), each with its own unique SKU, price, and stock
 * quantity.
 *
 * <p>The {@code sku} column has a {@code UNIQUE} constraint at the database
 * level, combined with service-layer checks to give clear error messages
 * before violating the constraint.
 *
 * <p>The {@code stockQuantity} column has a {@code CHECK (stock_quantity >= 0)}
 * constraint at the database level as a last line of defense, alongside
 * {@code InventoryService} validation. The {@link #version} field enables
 * optimistic locking so concurrent order placements cannot over-decrement
 * stock.
 *
 * <p>A variant is "soft-deleted" by setting {@code active} to
 * {@code false} instead of being removed from the database, so that
 * {@code OrderItem} snapshots referencing this variant remain valid even
 * after it is hidden from product listings and the Admin "edit product"
 * variant table.
 */
@Entity
@Table(name = "product_variants")
@Check(constraints = "stock_quantity >= 0")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductVariant {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(columnDefinition = "TEXT")
    private String attributes;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    /**
     * Soft-delete flag. A Biến_Thể_Sản_Phẩm is "removed" by setting this
     * to {@code false} instead of deleting the row, because
     * {@code OrderItem.variant} is a direct {@code @ManyToOne} reference
     * (not an independent snapshot id) — hard-deleting a variant that was
     * ever ordered would violate the foreign key or corrupt historical
     * order data. Mirrors the same pattern used by {@link Product#isActive()}.
     */
    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Setter(AccessLevel.NONE)
    private long version;

    public ProductVariant(Product product, String sku, String attributes, BigDecimal price, int stockQuantity) {
        this.product = product;
        this.sku = sku;
        this.attributes = attributes;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }
}
