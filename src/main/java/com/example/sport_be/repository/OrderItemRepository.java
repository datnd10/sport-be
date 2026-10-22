package com.example.sport_be.repository;

import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrder_Id(UUID orderId);

    /**
     * Finds an {@link OrderItem} that makes {@code accountId} eligible to
     * review {@code productId}: the item's variant must belong to that
     * product, the item's order must belong to that account, and the
     * order's status must be {@code status} (Requirement 9.1, 9.2).
     *
     * <p>Not already-reviewed is checked separately via {@code
     * ProductReviewRepository#findByOrderItem_Id}, since a customer may
     * have multiple eligible, unreviewed order items for the same
     * product (e.g. bought it in two different delivered orders).
     */
    @Query("SELECT oi FROM OrderItem oi " +
            "WHERE oi.order.account.id = :accountId " +
            "AND oi.variant.product.id = :productId " +
            "AND oi.order.status = :status")
    List<OrderItem> findEligibleForReview(@Param("accountId") UUID accountId,
                                           @Param("productId") UUID productId,
                                           @Param("status") OrderStatus status);

    /**
     * Aggregates total quantity sold per Sản_Phẩm for Đơn_Hàng created
     * within {@code [from, to)}, excluding Đã_Hủy orders, sorted
     * descending by total quantity sold (Requirement 10.3, Property 34).
     */
    @Query("SELECT oi.variant.product.id, oi.variant.product.name, SUM(oi.quantity) " +
            "FROM OrderItem oi " +
            "WHERE oi.order.createdAt >= :from AND oi.order.createdAt < :to " +
            "AND oi.order.status <> :excludedStatus " +
            "GROUP BY oi.variant.product.id, oi.variant.product.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> sumQuantityByProductInRange(@Param("from") Instant from,
                                                @Param("to") Instant to,
                                                @Param("excludedStatus") OrderStatus excludedStatus);

    /**
     * Aggregates revenue (quantity × unit price snapshot at purchase
     * time) per Danh_Mục for Đơn_Hàng created within {@code [from, to)},
     * excluding Đã_Hủy orders, sorted descending by revenue (Requirement
     * 10). Uses the price snapshot captured on the order item rather
     * than the product/variant's current price, since prices may have
     * changed since the order was placed.
     */
    @Query("SELECT oi.variant.product.category.id, oi.variant.product.category.name, " +
            "SUM(oi.quantity * oi.unitPriceSnapshot) " +
            "FROM OrderItem oi " +
            "WHERE oi.order.createdAt >= :from AND oi.order.createdAt < :to " +
            "AND oi.order.status <> :excludedStatus " +
            "GROUP BY oi.variant.product.category.id, oi.variant.product.category.name " +
            "ORDER BY SUM(oi.quantity * oi.unitPriceSnapshot) DESC")
    List<Object[]> sumRevenueByCategoryInRange(@Param("from") Instant from,
                                                @Param("to") Instant to,
                                                @Param("excludedStatus") OrderStatus excludedStatus);
}
