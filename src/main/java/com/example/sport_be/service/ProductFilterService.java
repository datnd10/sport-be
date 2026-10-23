package com.example.sport_be.service;

import com.example.sport_be.dto.category.CategoryResponse;
import com.example.sport_be.dto.product.ProductFilterCriteria;
import com.example.sport_be.dto.product.ProductResponse;
import com.example.sport_be.dto.product.ProductSearchResult;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.repository.CategoryRepository;
import com.example.sport_be.repository.ProductImageRepository;
import com.example.sport_be.repository.ProductRepository;
import com.example.sport_be.repository.ProductReviewRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Handles keyword search over Sản_Phẩm name/description (Requirement
 * 2.3), combined filtering by price range, Danh_Mục, and
 * Biến_Thể_Sản_Phẩm attributes (Requirement 2.6), and falls back to
 * popular Danh_Mục suggestions when a search yields no results
 * (Requirement 2.4).
 *
 * <p>Only reads {@code Product}/{@code ProductVariant} data; never
 * mutates {@code stockQuantity} (that remains the exclusive
 * responsibility of {@link InventoryService}).
 */
@Service
public class ProductFilterService {

    private static final int POPULAR_CATEGORY_SUGGESTION_LIMIT = 5;

    /**
     * Default ordering applied to search/filter results: most recently
     * created Sản_Phẩm first. This also lets the homepage reuse this
     * same listing (with no filters applied) to show "Sản_Phẩm nổi bật"
     * (featured products) as the newest active products, without needing
     * a separate "featured" flag/endpoint (Requirement 2.1).
     */
    private static final Sort MOST_RECENT_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;

    public ProductFilterService(ProductRepository productRepository,
                                 ProductVariantRepository productVariantRepository,
                                 CategoryRepository categoryRepository,
                                 ProductImageRepository productImageRepository,
                                 ProductReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.productImageRepository = productImageRepository;
    }

    /**
     * Searches/filters active Sản_Phẩm according to {@code criteria}.
     *
     * <p>The result contains exactly the active products satisfying
     * every applied condition in {@code criteria} (keyword, category,
     * price range, variant attributes) — no more, no less (Property 6).
     * When a keyword was supplied and no product matches, the result
     * instead carries a list of popular Danh_Mục suggestions
     * (Requirement 2.4).
     */
    @Transactional(readOnly = true)
    public ProductSearchResult search(ProductFilterCriteria criteria) {
        List<Product> products = productRepository.findAll(buildSpecification(criteria), MOST_RECENT_FIRST);

        Map<UUID, Object[]> ratings = new HashMap<>();
        if (!products.isEmpty()) {
            for (Object[] row : reviewRepository.summarizeRatings(products.stream().map(Product::getId).toList())) {
                ratings.put((UUID) row[0], row);
            }
        }
        Comparator<ProductResponse> ordering = switch (criteria.sort() == null ? "newest" : criteria.sort()) {
            case "price_asc" -> Comparator.comparing(ProductResponse::basePrice);
            case "price_desc" -> Comparator.comparing(ProductResponse::basePrice).reversed();
            case "rating_desc" -> Comparator.comparingDouble(ProductResponse::averageRating).reversed();
            default -> (a, b) -> 0;
        };
        List<ProductResponse> responses = products.stream()
                .map(product -> ProductResponse.from(
                        product,
                        productVariantRepository.findByProduct_IdAndActiveTrue(product.getId()),
                        productImageRepository.findByProduct_IdOrderByDisplayOrderAsc(product.getId())))
                .map(response -> {
                    Object[] summary = ratings.get(response.id());
                    return summary == null ? response : response.withRating(((Number) summary[1]).doubleValue(), ((Number) summary[2]).longValue());
                })
                .filter(response -> criteria.minRating() == null || (response.reviewCount() > 0 && response.averageRating() >= criteria.minRating()))
                .sorted(ordering)
                .toList();

        if (responses.isEmpty() && criteria.hasKeyword()) {
            return ProductSearchResult.noResults(getPopularCategories());
        }
        return ProductSearchResult.withProducts(responses);
    }

    /** Filters, orders and pages in the database before loading card details. */
    @Transactional(readOnly = true)
    public ProductSearchResult searchPaged(ProductFilterCriteria criteria, int page, int size) {
        Specification<Product> spec = buildSpecification(criteria);
        if (criteria.minRating() != null) {
            spec = spec.and((root, query, cb) -> cb.ge(averageRating(root, query, cb), criteria.minRating()));
        }
        spec = spec.and((root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                var primary = switch (criteria.sort() == null ? "newest" : criteria.sort()) {
                    case "price_asc" -> cb.asc(root.get("basePrice"));
                    case "price_desc" -> cb.desc(root.get("basePrice"));
                    case "rating_desc" -> cb.desc(averageRating(root, query, cb));
                    default -> cb.desc(root.get("createdAt"));
                };
                query.orderBy(primary, cb.desc(root.get("createdAt")), cb.asc(root.get("id")));
            }
            return cb.conjunction();
        });
        var result = productRepository.findAll(spec, PageRequest.of(page, size));
        if (page > 0 && result.getTotalPages() <= page) {
            result = productRepository.findAll(spec, PageRequest.of(Math.max(0, result.getTotalPages() - 1), size));
        }
        Map<UUID, Object[]> ratings = new HashMap<>();
        if (!result.isEmpty()) {
            for (Object[] row : reviewRepository.summarizeRatings(result.getContent().stream().map(Product::getId).toList())) {
                ratings.put((UUID) row[0], row);
            }
        }
        var responses = result.getContent().stream().map(product -> {
            var response = ProductResponse.from(product,
                    productVariantRepository.findByProduct_IdAndActiveTrue(product.getId()),
                    productImageRepository.findByProduct_IdOrderByDisplayOrderAsc(product.getId()));
            var summary = ratings.get(product.getId());
            return summary == null ? response : response.withRating(((Number) summary[1]).doubleValue(), ((Number) summary[2]).longValue());
        }).toList();
        return new ProductSearchResult(responses,
                result.getTotalElements() == 0 && criteria.hasKeyword() ? getPopularCategories() : List.of(),
                result.getTotalElements(), result.getTotalPages(), result.getNumber(), size);
    }

    private jakarta.persistence.criteria.Expression<Double> averageRating(
            jakarta.persistence.criteria.Root<Product> root, jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb) {
        Subquery<Double> average = query.subquery(Double.class);
        var review = average.from(com.example.sport_be.entity.ProductReview.class);
        average.select(cb.avg(review.get("rating")));
        average.where(cb.equal(review.get("product"), root), cb.isFalse(review.get("hidden")));
        return cb.coalesce(average, 0.0);
    }

    /**
     * Returns the most popular Danh_Mục (highest active product count),
     * used to suggest alternatives when a keyword search finds nothing
     * (Requirement 2.4).
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getPopularCategories() {
        return categoryRepository.findPopularCategories(PageRequest.of(0, POPULAR_CATEGORY_SUGGESTION_LIMIT))
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    private Specification<Product> buildSpecification(ProductFilterCriteria criteria) {
        Specification<Product> spec = (root, query, cb) -> cb.isTrue(root.get("active"));

        if (criteria.hasKeyword()) {
            String pattern = "%" + criteria.keyword().trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern)
            ));
        }

        if (criteria.categoryId() != null) {
            UUID categoryId = criteria.categoryId();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }

        if (criteria.minPrice() != null || criteria.maxPrice() != null
                || !criteria.variantAttributes().isEmpty()) {
            spec = spec.and(variantMatchSpecification(criteria));
        }

        return spec;
    }

    /**
     * Builds a specification requiring that the product has at least one
     * {@link ProductVariant} satisfying the price range AND every
     * requested attribute key/value pair simultaneously (as opposed to
     * independently matching different variants for price vs.
     * attributes), so the combined price+attribute filter reflects a
     * single real variant a Khách_Hàng could actually buy.
     *
     * <p>Expressed as an {@code EXISTS} subquery against
     * {@code ProductVariant} rather than a direct join, since
     * {@link Product} does not declare a mapped {@code variants}
     * collection (only the inverse {@code @ManyToOne} side exists on
     * {@link ProductVariant}).
     */
    private Specification<Product> variantMatchSpecification(ProductFilterCriteria criteria) {
        return (root, query, cb) -> {
            Subquery<UUID> subquery = query.subquery(UUID.class);
            var variant = subquery.from(ProductVariant.class);
            subquery.select(variant.get("id"));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(variant.get("product"), root));
            predicates.add(cb.isTrue(variant.get("active")));

            if (criteria.minPrice() != null) {
                predicates.add(cb.ge(variant.get("price"), criteria.minPrice()));
            }
            if (criteria.maxPrice() != null) {
                predicates.add(cb.le(variant.get("price"), criteria.maxPrice()));
            }
            for (Map.Entry<String, String> attribute : criteria.variantAttributes().entrySet()) {
                String pair = (attribute.getKey() + "=" + attribute.getValue()).toLowerCase(Locale.ROOT);
                predicates.add(cb.like(cb.lower(cb.coalesce(variant.get("attributes"), "")), "%" + pair + "%"));
            }

            subquery.where(predicates.toArray(new Predicate[0]));
            return cb.exists(subquery);
        };
    }
}
