package com.example.sport_be.service;

import com.example.sport_be.dto.category.CategoryResponse;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.entity.Category;
import com.example.sport_be.exception.ConflictException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.CategoryRepository;
import com.example.sport_be.utils.SlugUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles listing, creating, and updating {@link Category} records
 * (Requirement 1.1).
 */
@Service
public class CategoryService {

    public static final String DUPLICATE_CATEGORY_ERROR_CODE = "CATEGORY_NAME_ALREADY_EXISTS";
    private static final String CATEGORY_NOT_FOUND_MESSAGE = "Không tìm thấy Danh_Mục.";

    /**
     * Default ordering applied to the Admin Danh_Mục management list:
     * name ascending. Unlike Sản_Phẩm (sorted newest-first), Danh_Mục are
     * few and rarely change, so sorting alphabetically by name is more
     * useful for an Admin scanning the list.
     */
    private static final Sort ADMIN_LIST_DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "name");

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns every {@link Category} currently stored, for the public
     * category list (Requirement 1.1).
     */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Returns a paginated, optionally keyword-filtered page of
     * {@link Category}, for the Admin Danh_Mục management list.
     *
     * <p>{@code keyword} performs a case-insensitive, partial-match
     * search against the Danh_Mục name, applied at the database level
     * via {@link Specification} so the full table is never loaded into
     * memory. Results are sorted by name ascending.
     *
     * @param page    0-based page index
     * @param size    page size
     * @param keyword optional case-insensitive, partial-match filter on
     *                the Danh_Mục name
     */
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> findAllForAdmin(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, ADMIN_LIST_DEFAULT_SORT);
        Specification<Category> spec = buildAdminListSpecification(keyword);

        Page<Category> categoryPage = categoryRepository.findAll(spec, pageable);

        return PageResponse.from(categoryPage, CategoryResponse::from);
    }

    private Specification<Category> buildAdminListSpecification(String keyword) {
        Specification<Category> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
        }

        return spec;
    }

    /**
     * Creates a new {@link Category} with the given name, deriving a
     * unique, URL-friendly slug from it, and persists it so it
     * immediately shows up in the category list (Requirement 1.1).
     *
     * @throws ConflictException if a category with the same derived slug
     *                           already exists
     */
    @Transactional
    public Category createCategory(String name) {
        String slug = SlugUtils.toSlug(name);

        if (categoryRepository.findBySlug(slug).isPresent()) {
            throw new ConflictException(DUPLICATE_CATEGORY_ERROR_CODE,
                    "Danh_Mục với tên này đã tồn tại.");
        }

        Category category = new Category(name, slug);
        return categoryRepository.save(category);
    }

    /**
     * Renames an existing {@link Category}, recomputing its slug from
     * the new name. Deliberately does not support deletion: an existing
     * Danh_Mục may be referenced by active Sản_Phẩm records, and removing
     * it would corrupt that data, so renaming is the only mutation
     * offered besides creation.
     *
     * @throws ResourceNotFoundException if no category with {@code id} exists
     * @throws ConflictException         if the new name's derived slug is
     *                                    already used by a different category
     */
    @Transactional
    public CategoryResponse update(UUID id, String newName) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND_MESSAGE));

        String newSlug = SlugUtils.toSlug(newName);

        if (!newSlug.equals(category.getSlug())) {
            categoryRepository.findBySlug(newSlug).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new ConflictException(DUPLICATE_CATEGORY_ERROR_CODE,
                            "Danh_Mục với tên này đã tồn tại.");
                }
            });
        }

        category.setName(newName);
        category.setSlug(newSlug);

        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }
}
