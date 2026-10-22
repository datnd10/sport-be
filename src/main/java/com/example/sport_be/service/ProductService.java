package com.example.sport_be.service;

import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.product.AddProductVariantRequest;
import com.example.sport_be.dto.product.ProductCreateRequest;
import com.example.sport_be.dto.product.ProductDetailResponse;
import com.example.sport_be.dto.product.ProductResponse;
import com.example.sport_be.dto.product.ProductUpdateRequest;
import com.example.sport_be.dto.product.ProductVariantRequest;
import com.example.sport_be.dto.product.UpdateProductVariantRequest;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductImage;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.exception.ConflictException;
import com.example.sport_be.exception.FieldError;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.CategoryRepository;
import com.example.sport_be.repository.ProductImageRepository;
import com.example.sport_be.repository.ProductRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Handles creating and updating {@link Product} records together with
 * their initial {@link ProductVariant}s (Requirement 1.2, 1.3, 1.4, 1.6,
 * 1.7).
 *
 * <p>Initial stock for a newly created variant is set exclusively through
 * {@link InventoryService#adjustManually(UUID, int)} rather than by
 * writing {@code stockQuantity} directly, so every initial stock value is
 * captured in {@code InventoryHistory} (Requirement 7.5) just like any
 * other stock change.
 */
@Service
public class ProductService {

    public static final String PRODUCT_NAME_REQUIRED_ERROR_CODE = "PRODUCT_NAME_REQUIRED";
    public static final String PRODUCT_PRICE_REQUIRED_ERROR_CODE = "PRODUCT_PRICE_REQUIRED";
    public static final String DUPLICATE_SKU_ERROR_CODE = "DUPLICATE_SKU";
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Không tìm thấy Danh_Mục.";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Không tìm thấy Sản_Phẩm.";
    private static final String VARIANT_NOT_FOUND_MESSAGE = "Không tìm thấy Biến_Thể_Sản_Phẩm.";

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryService inventoryService;

    public ProductService(ProductRepository productRepository,
                           ProductVariantRepository productVariantRepository,
                           CategoryRepository categoryRepository,
                           ProductImageRepository productImageRepository,
                           InventoryService inventoryService) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Creates a new {@link Product} together with at least one initial
     * {@link ProductVariant} (Requirement 1.2, 1.6).
     *
     * <p>Validates that the name and price are present (Requirement 1.3),
     * that the referenced category exists, and that none of the
     * requested SKUs already exist (Requirement 1.7) before persisting
     * anything, so a rejected request never leaves partial data behind.
     *
     * @throws ValidationException       if the name/price are missing or invalid
     * @throws ResourceNotFoundException if the category does not exist
     * @throws ConflictException         if any requested SKU already exists
     */
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        validateNameAndPrice(request.name(), request.basePrice());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND_MESSAGE));

        List<ProductVariantRequest> variantRequests = request.variants();
        ensureSkusAreAvailable(variantRequests);

        Product product = new Product(category, request.name(), request.basePrice());
        product.setDescription(request.description());
        Product savedProduct = productRepository.save(product);

        List<ProductVariant> savedVariants = new ArrayList<>(variantRequests.size());
        for (ProductVariantRequest variantRequest : variantRequests) {
            savedVariants.add(createVariant(savedProduct, variantRequest));
        }

        return ProductResponse.from(savedProduct, savedVariants, List.of());
    }

    /**
     * Updates an existing {@link Product}'s own fields (name, description,
     * price, category) as a straightforward round-trip update
     * (Requirement 1.4). Does not touch variants or {@code stockQuantity};
     * stock changes must go through {@link InventoryService}.
     *
     * @throws ValidationException       if the name/price are missing or invalid
     * @throws ResourceNotFoundException if the product or category does not exist
     */
    @Transactional
    public ProductResponse update(UUID productId, ProductUpdateRequest request) {
        validateNameAndPrice(request.name(), request.basePrice());

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND_MESSAGE));

        product.setCategory(category);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());

        Product savedProduct = productRepository.save(product);
        List<ProductVariant> variants = productVariantRepository.findByProduct_IdAndActiveTrue(savedProduct.getId());
        List<ProductImage> images = productImageRepository.findByProduct_IdOrderByDisplayOrderAsc(savedProduct.getId());

        return ProductResponse.from(savedProduct, variants, images);
    }

    /**
     * Soft-deletes a {@link Product} by setting {@code active = false}
     * (Requirement 1.5). The {@code Product} row itself and any
     * {@code OrderItem} snapshots referencing it are left untouched, so
     * existing orders keep displaying accurate historical data even
     * after the product is hidden from public listings.
     *
     * @throws ResourceNotFoundException if the product does not exist
     */
    @Transactional
    public void delete(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Adds a new {@link ProductVariant} to an existing {@link Product}
     * (Requirement 1.6), rejecting the request if the SKU is already in
     * use (Requirement 1.7). Initial stock is set exclusively through
     * {@link InventoryService#adjustManually(UUID, int)} (Requirement 7.5).
     *
     * @throws ResourceNotFoundException if the product does not exist
     * @throws ConflictException         if the requested SKU already exists
     */
    @Transactional
    public ProductResponse addVariant(UUID productId, AddProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        if (productVariantRepository.existsBySku(request.sku())) {
            throw new ConflictException(DUPLICATE_SKU_ERROR_CODE,
                    "Mã SKU '" + request.sku() + "' đã tồn tại.");
        }

        ProductVariantRequest variantRequest = new ProductVariantRequest(
                request.sku(), request.attributes(), request.price(),
                request.stockQuantity(), request.lowStockThreshold());
        createVariant(product, variantRequest);

        List<ProductVariant> variants = productVariantRepository.findByProduct_IdAndActiveTrue(product.getId());
        List<ProductImage> images = productImageRepository.findByProduct_IdOrderByDisplayOrderAsc(product.getId());
        return ProductResponse.from(product, variants, images);
    }

    /**
     * Updates an existing {@link ProductVariant}'s own fields: SKU,
     * attributes, price, and low-stock threshold. Deliberately never
     * touches {@code stockQuantity} under any circumstances; Tồn_Kho
     * changes must go exclusively through
     * {@link InventoryService#adjustManually(UUID, int)} so every stock
     * change keeps its {@code InventoryHistory} audit trail
     * (Requirement 7.5).
     *
     * <p>The variant must belong to the given {@code productId}; a
     * variant that exists but belongs to a different product is treated
     * the same as a non-existent variant, so an Admin cannot edit a
     * variant through a mismatched productId in the URL.
     *
     * @throws ResourceNotFoundException if the product does not exist, the
     *                                    variant does not exist, or the
     *                                    variant does not belong to the
     *                                    given product
     * @throws ConflictException         if the new SKU is already used by a
     *                                    different variant
     */
    @Transactional
    public ProductResponse updateVariant(UUID productId, UUID variantId, UpdateProductVariantRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(VARIANT_NOT_FOUND_MESSAGE));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException(VARIANT_NOT_FOUND_MESSAGE);
        }

        String newSku = request.sku();
        if (!newSku.equals(variant.getSku()) && productVariantRepository.existsBySku(newSku)) {
            throw new ConflictException(DUPLICATE_SKU_ERROR_CODE,
                    "Mã SKU '" + newSku + "' đã tồn tại.");
        }

        variant.setSku(newSku);
        variant.setAttributes(request.attributes());
        variant.setPrice(request.price());
        if (request.lowStockThreshold() != null) {
            variant.setLowStockThreshold(request.lowStockThreshold());
        }

        productVariantRepository.save(variant);

        List<ProductVariant> variants = productVariantRepository.findByProduct_IdAndActiveTrue(product.getId());
        List<ProductImage> images = productImageRepository.findByProduct_IdOrderByDisplayOrderAsc(product.getId());
        return ProductResponse.from(product, variants, images);
    }

    /**
     * Soft-deletes a {@link ProductVariant} by setting {@code active = false}
     * (simplified Admin variant table: only view/add/delete — direct field
     * editing of an existing variant is no longer exposed in the UI, though
     * {@link #updateVariant} remains available for other callers).
     *
     * <p>The variant row itself, its {@code stockQuantity}, and any
     * {@code OrderItem} snapshots referencing it are left untouched, since
     * {@code OrderItem.variant} is a direct {@code @ManyToOne} reference —
     * hard-deleting a variant that was ever ordered would violate the
     * foreign key or corrupt historical order data.
     *
     * <p>The variant must belong to the given {@code productId}; a variant
     * that exists but belongs to a different product is treated the same
     * as a non-existent variant, consistent with {@link #updateVariant}.
     *
     * @throws ResourceNotFoundException if the product does not exist, the
     *                                    variant does not exist, or the
     *                                    variant does not belong to the
     *                                    given product
     */
    @Transactional
    public ProductResponse deleteVariant(UUID productId, UUID variantId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException(VARIANT_NOT_FOUND_MESSAGE));

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException(VARIANT_NOT_FOUND_MESSAGE);
        }

        variant.setActive(false);
        productVariantRepository.save(variant);

        List<ProductVariant> variants = productVariantRepository.findByProduct_IdAndActiveTrue(product.getId());
        List<ProductImage> images = productImageRepository.findByProduct_IdOrderByDisplayOrderAsc(product.getId());
        return ProductResponse.from(product, variants, images);
    }

    /**
     * Default ordering applied to the Admin product management list when
     * no other convention applies: Sản_Phẩm createdAt descending (newest
     * first).
     */
    private static final Sort ADMIN_LIST_DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    /**
     * Whitelist of Sản_Phẩm fields the Admin product management list may
     * be sorted by. Deliberately restricted (rather than accepting any
     * client-supplied field name) to avoid exposing internal/unmapped
     * field names or triggering a {@code PropertyReferenceException}.
     */
    private static final Set<String> ADMIN_LIST_SORTABLE_FIELDS = Set.of("name", "basePrice", "createdAt");

    /**
     * Returns a page of {@link Product}, regardless of its {@code active}
     * (soft-delete) flag, for the Admin product management list, so a
     * Quản_Trị_Viên can see and manage both currently active and
     * previously deleted products in one place (Requirement 1.4, 1.5).
     *
     * <p>Optionally filtered by a case-insensitive, partial-match
     * {@code keyword} against the Sản_Phẩm name and/or a {@code categoryId}
     * (Danh_Mục), both applied at the database level via
     * {@link ProductRepository}'s {@link Specification} support so the
     * full table is never loaded into memory.
     *
     * <p>Sorting defaults to createdAt descending (newest first) unless
     * {@code sortBy} names one of {@link #ADMIN_LIST_SORTABLE_FIELDS}, in
     * which case that field is used together with {@code sortDir}
     * ({@code asc}/{@code desc}, case-insensitive, defaulting to
     * descending). Any unrecognized {@code sortBy} silently falls back to
     * the default sort rather than failing the request.
     *
     * @param page       0-based page index
     * @param size       page size
     * @param keyword    optional case-insensitive, partial-match filter on
     *                   the Sản_Phẩm name
     * @param categoryId optional filter restricting results to a single
     *                   Danh_Mục
     * @param sortBy     optional field to sort by; must be one of
     *                   {@code name}, {@code basePrice}, {@code createdAt}
     * @param sortDir    optional sort direction ({@code asc}/{@code desc});
     *                   only used together with a valid {@code sortBy}
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAllForAdmin(int page, int size, String keyword, UUID categoryId,
                                                           String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size, resolveAdminListSort(sortBy, sortDir));
        Specification<Product> spec = buildAdminListSpecification(keyword, categoryId);

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        return PageResponse.from(productPage,
                product -> ProductResponse.from(
                        product,
                        productVariantRepository.findByProduct_IdAndActiveTrue(product.getId()),
                        productImageRepository.findByProduct_IdOrderByDisplayOrderAsc(product.getId())));
    }

    /**
     * Overload preserving the previous default-sort-only behavior for
     * existing callers.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAllForAdmin(int page, int size, String keyword, UUID categoryId) {
        return findAllForAdmin(page, size, keyword, categoryId, null, null);
    }

    private Sort resolveAdminListSort(String sortBy, String sortDir) {
        if (sortBy == null || !ADMIN_LIST_SORTABLE_FIELDS.contains(sortBy)) {
            return ADMIN_LIST_DEFAULT_SORT;
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, sortBy);
    }

    private Specification<Product> buildAdminListSpecification(String keyword, UUID categoryId) {
        Specification<Product> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }

        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }

        return spec;
    }

    /**
     * Returns the full public-facing detail of a {@link Product}: its
     * Danh_Mục, all Biến_Thể_Sản_Phẩm (with stock/out-of-stock status),
     * and Hình_Ảnh_Sản_Phẩm, for the product detail page
     * (Requirement 2.5).
     *
     * @throws ResourceNotFoundException if the product does not exist
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getDetail(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND_MESSAGE));

        List<ProductVariant> variants = productVariantRepository.findByProduct_IdAndActiveTrue(productId);
        List<ProductImage> images = productImageRepository.findByProduct_IdOrderByDisplayOrderAsc(productId);

        return ProductDetailResponse.from(product, variants, images);
    }

    private ProductVariant createVariant(Product product, ProductVariantRequest request) {
        ProductVariant variant = new ProductVariant(
                product,
                request.sku(),
                request.attributes(),
                request.price(),
                0
        );
        if (request.lowStockThreshold() != null) {
            variant.setLowStockThreshold(request.lowStockThreshold());
        }
        ProductVariant savedVariant = productVariantRepository.save(variant);

        if (request.stockQuantity() > 0) {
            return inventoryService.adjustManually(savedVariant.getId(), request.stockQuantity());
        }
        return savedVariant;
    }

    private void ensureSkusAreAvailable(List<ProductVariantRequest> variantRequests) {
        for (ProductVariantRequest variantRequest : variantRequests) {
            if (productVariantRepository.existsBySku(variantRequest.sku())) {
                throw new ConflictException(DUPLICATE_SKU_ERROR_CODE,
                        "Mã SKU '" + variantRequest.sku() + "' đã tồn tại.");
            }
        }
    }

    private void validateNameAndPrice(String name, BigDecimal price) {
        List<FieldError> fieldErrors = new ArrayList<>();

        if (name == null || name.isBlank()) {
            fieldErrors.add(new FieldError("name", "Tên Sản_Phẩm không được để trống"));
        }
        if (price == null) {
            fieldErrors.add(new FieldError("basePrice", "Giá Sản_Phẩm không được để trống"));
        } else if (price.signum() <= 0) {
            fieldErrors.add(new FieldError("basePrice", "Giá Sản_Phẩm phải lớn hơn 0"));
        }

        if (!fieldErrors.isEmpty()) {
            String errorCode = (name == null || name.isBlank())
                    ? PRODUCT_NAME_REQUIRED_ERROR_CODE
                    : PRODUCT_PRICE_REQUIRED_ERROR_CODE;
            throw new ValidationException(errorCode, "Sản_Phẩm thiếu tên hoặc giá hợp lệ.", fieldErrors);
        }
    }
}
