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
 * Represents an image file attached to a {@link Product}, stored on local
 * disk (e.g. under {@code uploads/products/{productId}/}).
 *
 * <p>{@code filePath} holds the relative path to the stored file, and
 * {@code displayOrder} controls the order in which images are rendered on
 * the product detail page (ascending).
 */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public ProductImage(Product product, String filePath, int displayOrder) {
        this.product = product;
        this.filePath = filePath;
        this.displayOrder = displayOrder;
    }
}
