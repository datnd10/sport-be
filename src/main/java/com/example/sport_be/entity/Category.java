package com.example.sport_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Represents a product category used to classify {@link Product}s.
 *
 * <p>A category has a human-readable {@code name} and a unique
 * {@code slug} used for category-based navigation/filtering.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    public Category(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }
}
