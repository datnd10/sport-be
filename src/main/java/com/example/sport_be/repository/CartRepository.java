package com.example.sport_be.repository;

import com.example.sport_be.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByAccount_Id(UUID accountId);
}
