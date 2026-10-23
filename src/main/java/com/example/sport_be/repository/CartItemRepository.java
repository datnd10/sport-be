package com.example.sport_be.repository;

import com.example.sport_be.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCart_Id(UUID cartId);

    Optional<CartItem> findByCart_IdAndVariant_Id(UUID cartId, UUID variantId);

    /** Called in the order transaction: failed orders must retain the cart. */
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id IN " +
            "(SELECT c.id FROM Cart c WHERE c.account.id = :accountId) AND ci.variant.id IN :variantIds")
    int deletePurchasedItems(@Param("accountId") UUID accountId, @Param("variantIds") List<UUID> variantIds);
}
