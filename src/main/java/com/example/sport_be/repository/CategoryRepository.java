package com.example.sport_be.repository;

import com.example.sport_be.entity.Category;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    Optional<Category> findBySlug(String slug);

    /**
     * Returns the most popular {@link Category} records, i.e. those with
     * the highest active product count, for the "popular Danh_Mục"
     * suggestions shown when a keyword search yields no results
     * (Requirement 2.4). The number of categories returned is controlled
     * by {@code pageable}'s page size.
     */
    @Query("""
            SELECT c FROM Category c
            JOIN Product p ON p.category = c AND p.active = true
            GROUP BY c
            ORDER BY COUNT(p) DESC
            """)
    List<Category> findPopularCategories(Pageable pageable);
}
