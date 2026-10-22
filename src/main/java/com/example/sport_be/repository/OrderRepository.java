package com.example.sport_be.repository;

import com.example.sport_be.entity.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    List<Order> findByAccount_Id(UUID accountId);

    List<Order> findByAccount_Id(UUID accountId, Sort sort);

    /**
     * Returns every Đơn_Hàng created within {@code [from, to)}, for the
     * Dashboard summary aggregation (Requirement 10.1, 10.2).
     */
    List<Order> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
}
