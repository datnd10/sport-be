package com.example.sport_be.repository;

import com.example.sport_be.entity.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, UUID> {

    List<InventoryHistory> findByVariant_Id(UUID variantId);

    List<InventoryHistory> findByVariant_IdOrderByChangedAtDesc(UUID variantId);
}
