package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.inventory.InventoryHistoryResponse;
import com.example.sport_be.dto.inventory.InventoryItemResponse;
import com.example.sport_be.dto.inventory.StockAdjustmentRequest;
import com.example.sport_be.entity.InventoryHistory;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.StockStatus;
import com.example.sport_be.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only endpoints for viewing and adjusting Tồn_Kho (Requirement 7).
 *
 * <p>Access is restricted to {@code ROLE_ADMIN} by {@code SecurityConfig},
 * which requires that role for every route under {@code /api/admin/**}.
 */
@RestController
@RequestMapping(ApiPaths.Inventory.BASE)
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /**
     * Returns a paginated, optionally keyword-filtered page of Tồn_Kho
     * items for the Admin Tồn_Kho management list (Requirement 7.3, 7.4).
     *
     * @param page    0-based page index (default 0)
     * @param size    page size (default 20)
     * @param keyword optional case-insensitive, partial-match search on
     *                SKU or the related Sản_Phẩm's name
     */
    @GetMapping
    public ResponseEntity<PageResponse<InventoryItemResponse>> listInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) StockStatus stockStatus) {
        return ResponseEntity.ok(inventoryService.listAllPaged(page, size, keyword, stockStatus));
    }

    @PutMapping(ApiPaths.Inventory.BY_VARIANT_ID)
    public ResponseEntity<InventoryItemResponse> adjustStock(
            @PathVariable UUID variantId,
            @Valid @RequestBody StockAdjustmentRequest request
    ) {
        ProductVariant variant = inventoryService.adjustManually(variantId, request.newQuantity());
        return ResponseEntity.ok(InventoryItemResponse.from(variant));
    }

    @GetMapping(ApiPaths.Inventory.HISTORY_BY_VARIANT_ID)
    public ResponseEntity<List<InventoryHistoryResponse>> getHistory(@PathVariable UUID variantId) {
        List<InventoryHistoryResponse> history = inventoryService.getHistory(variantId).stream()
                .map(InventoryHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }
}
