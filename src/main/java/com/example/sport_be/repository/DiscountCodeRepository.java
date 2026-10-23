package com.example.sport_be.repository;

import com.example.sport_be.entity.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID>, JpaSpecificationExecutor<DiscountCode> {

    Optional<DiscountCode> findByCode(String code);

    boolean existsByCode(String code);

    List<DiscountCode> findAllByOrderByValidFromDesc();

    @Query("SELECT d FROM DiscountCode d WHERE d.active = true AND d.validFrom <= :now " +
            "AND d.validTo >= :now AND d.usageCount < d.maxUsage ORDER BY d.validTo ASC, d.code ASC")
    List<DiscountCode> findAvailable(@Param("now") Instant now);
}
