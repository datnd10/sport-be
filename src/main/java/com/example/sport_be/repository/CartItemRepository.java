package com.example.sport_be.repository;

import com.example.sport_be.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCart_Id(UUID cartId);

    Optional<CartItem> findByCart_IdAndVariant_Id(UUID cartId, UUID variantId);
}
