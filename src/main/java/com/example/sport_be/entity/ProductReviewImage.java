package com.example.sport_be.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_review_images", uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "file_path"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductReviewImage {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_id", nullable = false)
    private ProductReview review;
    @Column(name = "file_path", nullable = false)
    private String filePath;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ProductReviewImage(ProductReview review, String filePath, int displayOrder) {
        this.review = review;
        this.filePath = filePath;
        this.displayOrder = displayOrder;
    }
}
