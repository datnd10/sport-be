package com.example.sport_be.repository;

import com.example.sport_be.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    List<OrderStatusHistory> findByOrder_Id(UUID orderId);
}
