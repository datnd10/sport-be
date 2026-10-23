package com.example.sport_be.service;
import com.example.sport_be.enums.StockStatus;

import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.inventory.InventoryItemResponse;
import com.example.sport_be.entity.InventoryHistory;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.InventoryChangeReason;
import com.example.sport_be.exception.BusinessRuleException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.InventoryHistoryRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The sole entry point for mutating {@link ProductVariant#getStockQuantity()}
 * (Requirement 7.1, 7.2, 7.5).
 *
 * <p>Every other module (Cart, Order, ...) MUST change stock exclusively
 * through this service rather than touching {@code ProductVariant} directly,
 * so that {@code stockQuantity} can never go negative and every change is
 * captured in {@link InventoryHistory}.
 */
@Service
public class InventoryService {

    public static final String NEGATIVE_QUANTITY_ERROR_CODE = "NEGATIVE_STOCK_QUANTITY";
    public static final String INSUFFICIENT_STOCK_ERROR_CODE = "INSUFFICIENT_STOCK";
    private static final String VARIANT_NOT_FOUND_MESSAGE = "Không tìm thấy Biến_Thể_Sản_Phẩm.";

    private final ProductVariantRepository productVariantRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    public InventoryService(ProductVariantRepository productVariantRepository,
                             InventoryHistoryRepository inventoryHistoryRepository) {
        this.productVariantRepository = productVariantRepository;
        this.inventoryHistoryRepository = inventoryHistoryRepository;
    }

    /**
     * Manually sets the stock quantity of a variant to {@code newQuantity}
     * (admin adjustment).
     *
     * <p>Rejects negative values and leaves {@code stockQuantity} unchanged
     * on rejection (Requirement 7.1, 7.2). On success, records an
     * {@link InventoryHistory} entry with reason
     * {@link InventoryChangeReason#MANUAL_ADJUSTMENT} and a
     * {@code changeAmount} equal to the signed difference applied
     * (Requirement 7.5).
     *
     * @throws ValidationException        if {@code newQuantity} is negative
     * @throws ResourceNotFoundException  if the variant does not exist
     */
    @Transactional
    public ProductVariant adjustManually(UUID variantId, int newQuantity) {
        if (newQuantity < 0) {
            throw new ValidationException(NEGATIVE_QUANTITY_ERROR_CODE,
                    "Số lượng Tồn_Kho không được âm.", Collections.emptyList());
        }

        ProductVariant variant = findVariantOrThrow(variantId);
        int changeAmount = newQuantity - variant.getStockQuantity();

        variant.setStockQuantity(newQuantity);
        ProductVariant saved = productVariantRepository.save(variant);

        if (changeAmount != 0) {
            recordHistory(saved, changeAmount, InventoryChangeReason.MANUAL_ADJUSTMENT);
        }

        return saved;
    }

    /**
     * Decreases stock when an order is placed (Requirement 4.2).
     *
     * <p>Never allows stock to go negative: if {@code quantity} exceeds the
     * current stock, the change is rejected and {@code stockQuantity} is
     * left unchanged. On success, records an {@link InventoryHistory}
     * entry with reason {@link InventoryChangeReason#ORDER_PLACED} and a
     * negative {@code changeAmount} (Requirement 7.5).
     *
     * @throws ValidationException        if {@code quantity} is not positive
     * @throws ResourceNotFoundException  if the variant does not exist
     * @throws BusinessRuleException      if {@code quantity} exceeds the current stock
     */
    @Transactional
    public ProductVariant decreaseForOrder(UUID variantId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException(NEGATIVE_QUANTITY_ERROR_CODE,
                    "Số lượng giảm Tồn_Kho phải lớn hơn 0.", Collections.emptyList());
        }

        ProductVariant variant = findVariantOrThrow(variantId);
        if (variant.getStockQuantity() < quantity) {
            throw new BusinessRuleException(INSUFFICIENT_STOCK_ERROR_CODE,
                    "Số lượng Tồn_Kho không đủ để đặt hàng.");
        }

        variant.setStockQuantity(variant.getStockQuantity() - quantity);
        ProductVariant saved = productVariantRepository.save(variant);

        recordHistory(saved, -quantity, InventoryChangeReason.ORDER_PLACED);

        return saved;
    }

    /**
     * Restores stock when an order is cancelled (Requirement 5.4).
     *
     * <p>Records an {@link InventoryHistory} entry with reason
     * {@link InventoryChangeReason#ORDER_CANCELLED} and a positive
     * {@code changeAmount} (Requirement 7.5).
     *
     * @throws ValidationException        if {@code quantity} is not positive
     * @throws ResourceNotFoundException  if the variant does not exist
     */
    @Transactional
    public ProductVariant restoreForCancellation(UUID variantId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException(NEGATIVE_QUANTITY_ERROR_CODE,
                    "Số lượng hoàn trả Tồn_Kho phải lớn hơn 0.", Collections.emptyList());
        }

        ProductVariant variant = findVariantOrThrow(variantId);
        variant.setStockQuantity(variant.getStockQuantity() + quantity);
        ProductVariant saved = productVariantRepository.save(variant);

        recordHistory(saved, quantity, InventoryChangeReason.ORDER_CANCELLED);

        return saved;
    }

    /**
     * Lists every {@link ProductVariant} with its current stock state, for
     * the admin inventory overview (Requirement 7.3, 7.4).
     */
    @Transactional(readOnly = true)
    public List<ProductVariant> listAll() {
        return productVariantRepository.findAll();
    }

    /**
     * Default ordering applied to the Admin Tồn_Kho management list:
     * SKU ascending. A join-based sort on the related Sản_Phẩm's name
     * is deliberately avoided to keep the query simple; SKU gives a
     * stable, predictable order that is sufficient for the Admin list.
     */
    private static final Sort ADMIN_LIST_DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "sku");

    /**
     * Returns a paginated, optionally keyword-filtered page of
     * {@link ProductVariant} with its current stock state, for the
     * Admin Tồn_Kho management list (Requirement 7.3, 7.4).
     *
     * <p>{@code keyword} performs a case-insensitive, partial-match
     * search against either the variant's SKU or its related
     * Sản_Phẩm's name, applied at the database level via
     * {@link Specification} (joining {@code product}) so the full
     * table is never loaded into memory. Results are sorted by SKU
     * ascending.
     *
     * @param page    0-based page index
     * @param size    page size
     * @param keyword optional case-insensitive, partial-match filter on
     *                SKU or Sản_Phẩm name
     */
    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> listAllPaged(int page, int size, String keyword) {
        return listAllPaged(page, size, keyword, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> listAllPaged(int page, int size, String keyword, StockStatus stockStatus) {
        Pageable pageable = PageRequest.of(page, size, ADMIN_LIST_DEFAULT_SORT);
        Specification<ProductVariant> spec = buildAdminListSpecification(keyword);

        if (stockStatus != null) {
            spec = spec.and((root, query, cb) -> {
                var quantity = root.<Integer>get("stockQuantity");
                var threshold = root.<Integer>get("lowStockThreshold");
                var stock = switch (stockStatus) {
                    case OUT_OF_STOCK -> cb.equal(quantity, 0);
                    case LOW_STOCK -> cb.and(cb.greaterThan(quantity, 0), cb.lessThan(quantity, threshold));
                    case IN_STOCK -> cb.and(cb.greaterThan(quantity, 0), cb.greaterThanOrEqualTo(quantity, threshold));
                };
                return cb.and(cb.isTrue(root.get("active")), cb.isTrue(root.get("product").get("active")), stock);
            });
        }

        Page<ProductVariant> variantPage = productVariantRepository.findAll(spec, pageable);

        return PageResponse.from(variantPage, InventoryItemResponse::from);
    }

    private Specification<ProductVariant> buildAdminListSpecification(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Specification.where(null);
        }

        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("sku")), pattern),
                cb.like(cb.lower(root.join("product").get("name")), pattern)
        );
    }

    /**
     * Returns a single variant's current stock state (Requirement 7.3, 7.4).
     *
     * @throws ResourceNotFoundException if the variant does not exist
     */
    @Transactional(readOnly = true)
    public ProductVariant getVariant(UUID variantId) {
        return findVariantOrThrow(variantId);
    }

    /**
     * Returns the full change history for a variant's stock quantity,
     * most recent first (Requirement 7.5).
     *
     * @throws ResourceNotFoundException if the variant does not exist
     */
    @Transactional(readOnly = true)
    public List<InventoryHistory> getHistory(UUID variantId) {
        findVariantOrThrow(variantId);
        return inventoryHistoryRepository.findByVariant_IdOrderByChangedAtDesc(variantId);
    }

    private ProductVariant findVariantOrThrow(UUID variantId) {
        return productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(VARIANT_NOT_FOUND_MESSAGE));
    }

    private void recordHistory(ProductVariant variant, int changeAmount, InventoryChangeReason reason) {
        inventoryHistoryRepository.save(new InventoryHistory(variant, changeAmount, reason));
    }
}
