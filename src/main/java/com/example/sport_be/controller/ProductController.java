package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.product.AddProductVariantRequest;
import com.example.sport_be.dto.product.ProductCreateRequest;
import com.example.sport_be.dto.product.ProductDetailResponse;
import com.example.sport_be.dto.product.ProductFilterCriteria;
import com.example.sport_be.dto.product.ProductImageResponse;
import com.example.sport_be.dto.product.ProductResponse;
import com.example.sport_be.dto.product.ProductSearchResult;
import com.example.sport_be.dto.product.ProductUpdateRequest;
import com.example.sport_be.dto.product.UpdateProductVariantRequest;
import com.example.sport_be.service.ProductFilterService;
import com.example.sport_be.service.ProductImageService;
import com.example.sport_be.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Endpoints for managing and viewing Sản_Phẩm (Product) records.
 *
 * <p>Routes under {@code /api/admin/**} require {@code ROLE_ADMIN}, per
 * {@code SecurityConfig}. {@code GET /api/products/{id}} is public.
 */
@RestController
public class ProductController {

    /**
     * Query parameter names reserved for the fixed filter fields, so any
     * other query parameter present on {@code GET /api/products} is
     * treated as a Biến_Thể_Sản_Phẩm attribute filter (e.g. {@code size=42}).
     */
    private static final Set<String> RESERVED_FILTER_PARAMS = Set.of("categoryId", "keyword", "minPrice", "maxPrice");

    private final ProductService productService;
    private final ProductImageService productImageService;
    private final ProductFilterService productFilterService;

    public ProductController(ProductService productService,
                              ProductImageService productImageService,
                              ProductFilterService productFilterService) {
        this.productService = productService;
        this.productImageService = productImageService;
        this.productFilterService = productFilterService;
    }

    /**
     * Returns the public list of active Sản_Phẩm, optionally filtered by
     * {@code categoryId}, {@code keyword}, price range
     * (Requirement 2.1, 2.2), and any Biến_Thể_Sản_Phẩm attribute passed
     * as an extra query parameter (e.g. {@code ?size=42&color=red}),
     * delegating the actual filtering logic to {@link ProductFilterService}
     * (Requirement 2.6). Also used to power the homepage's "featured
     * products" section by sorting/limiting on the client, or by simply
     * calling this endpoint without filters (Requirement 2.1).
     */
    @GetMapping(ApiPaths.Product.PUBLIC_LIST)
    public ResponseEntity<ProductSearchResult> listProducts(@RequestParam(required = false) UUID categoryId,
                                                              @RequestParam(required = false) String keyword,
                                                              @RequestParam(required = false) BigDecimal minPrice,
                                                              @RequestParam(required = false) BigDecimal maxPrice,
                                                              @RequestParam Map<String, String> allParams) {
        Map<String, String> variantAttributes = new LinkedHashMap<>(allParams);
        RESERVED_FILTER_PARAMS.forEach(variantAttributes::remove);

        ProductFilterCriteria criteria = new ProductFilterCriteria(keyword, categoryId, minPrice, maxPrice, variantAttributes);
        ProductSearchResult result = productFilterService.search(criteria);
        return ResponseEntity.ok(result);
    }

    /**
     * Returns a paginated, optionally filtered page of Sản_Phẩm
     * regardless of {@code active} status, for the Admin product
     * management list (Requirement 1.4, 1.5).
     *
     * <p>{@code keyword} performs a case-insensitive, partial-match
     * search against the Sản_Phẩm name; {@code categoryId} restricts
     * results to a single Danh_Mục. Both are optional and may be
     * combined. Results are sorted by {@code createdAt} descending by
     * default, or by {@code sortBy}/{@code sortDir} when provided (see
     * {@link ProductService#findAllForAdmin(int, int, String, UUID, String, String)}
     * for the allowed {@code sortBy} values and fallback behavior).
     *
     * @param page       0-based page index (default 0)
     * @param size       page size (default 20)
     * @param keyword    optional name search keyword
     * @param categoryId optional Danh_Mục filter
     * @param sortBy     optional column to sort by ({@code name},
     *                   {@code basePrice}, or {@code createdAt}); any
     *                   other value falls back to the default sort
     * @param sortDir    optional sort direction ({@code asc}/{@code desc},
     *                   case-insensitive; defaults to {@code desc})
     */
    @GetMapping(ApiPaths.Product.ADMIN_LIST)
    public ResponseEntity<PageResponse<ProductResponse>> listAllProductsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        return ResponseEntity.ok(productService.findAllForAdmin(page, size, keyword, categoryId, sortBy, sortDir));
    }

    /**
     * Creates a new Sản_Phẩm together with its initial Biến_Thể_Sản_Phẩm
     * list (Requirement 1.2, 1.6).
     */
    @PostMapping(ApiPaths.Product.ADMIN_CREATE)
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing Sản_Phẩm's own fields (Requirement 1.4).
     */
    @PutMapping(ApiPaths.Product.ADMIN_BY_ID)
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id,
                                                           @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse response = productService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes (hides) a Sản_Phẩm, keeping historical {@code OrderItem}
     * snapshots intact (Requirement 1.5).
     */
    @DeleteMapping(ApiPaths.Product.ADMIN_BY_ID)
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds a new Biến_Thể_Sản_Phẩm to an existing Sản_Phẩm (Requirement
     * 1.6), rejecting duplicate SKUs (Requirement 1.7).
     */
    @PostMapping(ApiPaths.Product.ADMIN_VARIANTS)
    public ResponseEntity<ProductResponse> addVariant(@PathVariable UUID id,
                                                        @Valid @RequestBody AddProductVariantRequest request) {
        ProductResponse response = productService.addVariant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing Biến_Thể_Sản_Phẩm's own fields: SKU,
     * attributes, price, and low-stock threshold. {@code stockQuantity}
     * is deliberately not part of this endpoint; Tồn_Kho changes must go
     * through {@code PUT /api/admin/inventory/{variantId}} so every stock
     * change keeps its {@code InventoryHistory} audit trail.
     */
    @PutMapping(ApiPaths.Product.ADMIN_VARIANT_BY_ID)
    public ResponseEntity<ProductResponse> updateVariant(@PathVariable UUID id,
                                                           @PathVariable UUID variantId,
                                                           @Valid @RequestBody UpdateProductVariantRequest request) {
        ProductResponse response = productService.updateVariant(id, variantId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft-deletes a Biến_Thể_Sản_Phẩm, hiding it from both the public
     * storefront and the Admin "edit product" variant table. The Admin
     * variant table only supports view/add/delete (no direct field
     * editing); Tồn_Kho adjustments for other variants remain on the
     * dedicated Inventory page.
     */
    @DeleteMapping(ApiPaths.Product.ADMIN_VARIANT_BY_ID)
    public ResponseEntity<ProductResponse> deleteVariant(@PathVariable UUID id,
                                                           @PathVariable UUID variantId) {
        ProductResponse response = productService.deleteVariant(id, variantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the full public-facing detail of a Sản_Phẩm: price,
     * description, images, available Biến_Thể_Sản_Phẩm and Tồn_Kho status
     * (Requirement 2.5).
     */
    @GetMapping(ApiPaths.Product.PUBLIC_BY_ID)
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable UUID id) {
        ProductDetailResponse response = productService.getDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Uploads one or more Hình_Ảnh_Sản_Phẩm for a Sản_Phẩm, validating
     * format and size before storing them on local disk (Requirement
     * 1.8, 1.10).
     */
    @PostMapping(ApiPaths.Product.ADMIN_IMAGES)
    public ResponseEntity<List<ProductImageResponse>> uploadImages(@PathVariable UUID id,
                                                                      @RequestParam("files") List<MultipartFile> files) {
        List<ProductImageResponse> response = productImageService.upload(id, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Deletes a Hình_Ảnh_Sản_Phẩm, removing both its database record and
     * the underlying file on disk (Requirement 1.9).
     */
    @DeleteMapping(ApiPaths.Product.ADMIN_IMAGE_BY_ID)
    public ResponseEntity<Void> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        productImageService.delete(id, imageId);
        return ResponseEntity.noContent().build();
    }
}