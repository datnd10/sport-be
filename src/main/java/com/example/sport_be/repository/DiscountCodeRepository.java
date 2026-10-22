package com.example.sport_be.repository;

import com.example.sport_be.entity.DiscountCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID>, JpaSpecificationExecutor<DiscountCode> {

    Optional<DiscountCode> findByCode(String code);

    boolean existsByCode(String code);

    List<DiscountCode> findAllByOrderByValidFromDesc();
}
