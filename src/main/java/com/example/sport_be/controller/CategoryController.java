package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.category.CategoryResponse;
import com.example.sport_be.dto.category.CreateCategoryRequest;
import com.example.sport_be.dto.category.UpdateCategoryRequest;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.entity.Category;
import com.example.sport_be.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints for listing and creating Danh_Mục (Category) records
 * (Requirement 1.1).
 *
 * <p>{@code GET /api/categories} is public per {@code SecurityConfig}.
 * {@code POST /api/admin/categories} lives under {@code /api/admin/**}
 * and therefore requires {@code ROLE_ADMIN}.
 */
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping(ApiPaths.Category.PUBLIC_LIST)
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories().stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(categories);
    }

    @PostMapping(ApiPaths.Category.ADMIN_CREATE)
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Category category = categoryService.createCategory(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }

    /**
     * Returns a paginated, optionally keyword-filtered page of Danh_Mục
     * for the Admin Danh_Mục management list.
     *
     * @param page    0-based page index (default 0)
     * @param size    page size (default 20)
     * @param keyword optional case-insensitive, partial-match search on
     *                the Danh_Mục name
     */
    @GetMapping(ApiPaths.Category.ADMIN_LIST)
    public ResponseEntity<PageResponse<CategoryResponse>> listAllCategoriesForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(categoryService.findAllForAdmin(page, size, keyword));
    }

    /**
     * Renames an existing Danh_Mục. Deletion is intentionally not
     * supported, since a Danh_Mục may be referenced by active Sản_Phẩm
     * records.
     */
    @PutMapping(ApiPaths.Category.ADMIN_BY_ID)
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id,
                                                              @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryResponse response = categoryService.update(id, request.name());
        return ResponseEntity.ok(response);
    }
}