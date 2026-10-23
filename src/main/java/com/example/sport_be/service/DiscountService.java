package com.example.sport_be.service;

import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.discount.DiscountCodeCreateRequest;
import com.example.sport_be.dto.discount.DiscountCodeResponse;
import com.example.sport_be.dto.discount.AvailableDiscountResponse;
import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.exception.BusinessRuleException;
import com.example.sport_be.exception.ConflictException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.DiscountCodeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles creating and managing {@link DiscountCode} records
 * (Requirement 8).
 */
@Service
public class DiscountService {

    public static final String DUPLICATE_DISCOUNT_CODE_ERROR_CODE = "DISCOUNT_CODE_ALREADY_EXISTS";
    public static final String DISCOUNT_CODE_NOT_FOUND_ERROR_CODE = "DISCOUNT_CODE_NOT_FOUND";
    public static final String DISCOUNT_CODE_INACTIVE_ERROR_CODE = "DISCOUNT_CODE_INACTIVE";
    public static final String DISCOUNT_CODE_NOT_YET_VALID_ERROR_CODE = "DISCOUNT_CODE_NOT_YET_VALID";
    public static final String DISCOUNT_CODE_EXPIRED_ERROR_CODE = "DISCOUNT_CODE_EXPIRED";
    public static final String DISCOUNT_CODE_USAGE_LIMIT_REACHED_ERROR_CODE = "DISCOUNT_CODE_USAGE_LIMIT_REACHED";

    private final DiscountCodeRepository discountCodeRepository;

    public DiscountService(DiscountCodeRepository discountCodeRepository) {
        this.discountCodeRepository = discountCodeRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailableDiscountResponse> findAvailable() {
        return discountCodeRepository.findAvailable(Instant.now()).stream()
                .map(AvailableDiscountResponse::from).toList();
    }

    /**
     * Creates a new {@link DiscountCode} and persists it so it is active
     * within its configured {@code [validFrom, validTo]} window
     * (Requirement 8.1).
     *
     * @throws ConflictException if a discount code with the same code
     *                           already exists (Requirement 8.2)
     */
    @Transactional
    public DiscountCode create(DiscountCodeCreateRequest request) {
        if (discountCodeRepository.existsByCode(request.code())) {
            throw new ConflictException(DUPLICATE_DISCOUNT_CODE_ERROR_CODE,
                    "Mã_Giảm_Giá với mã này đã tồn tại.");
        }

        DiscountCode discountCode = new DiscountCode(
                request.code(),
                request.discountType(),
                request.discountValue(),
                request.validFrom(),
                request.validTo(),
                request.maxUsage()
        );

        return discountCodeRepository.save(discountCode);
    }

    /**
     * Returns every {@link DiscountCode}, most recently valid-from first,
     * for the Admin Mã_Giảm_Giá management page (Requirement 8.1, 8.3).
     */
    @Transactional(readOnly = true)
    public List<DiscountCode> getAll() {
        return discountCodeRepository.findAllByOrderByValidFromDesc();
    }

    /**
     * Returns a page of {@link DiscountCode} records, most recently
     * valid-from first, optionally filtered by a case-insensitive
     * {@code code} substring, for the Admin Mã_Giảm_Giá management page
     * (Requirement 8.1, 8.3).
     *
     * @param page    the 0-based page index
     * @param size    the page size
     * @param keyword optional case-insensitive substring to match against
     *                {@code code}; ignored when blank
     */
    @Transactional(readOnly = true)
    public PageResponse<DiscountCodeResponse> findAllPaged(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "validFrom"));
        Specification<DiscountCode> spec = buildListSpecification(keyword);

        Page<DiscountCode> discountCodePage = discountCodeRepository.findAll(spec, pageable);

        return PageResponse.from(discountCodePage, DiscountCodeResponse::from);
    }

    private Specification<DiscountCode> buildListSpecification(String keyword) {
        Specification<DiscountCode> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("code")), pattern));
        }

        return spec;
    }

    /**
     * Deactivates a {@link DiscountCode} so it can no longer be applied to
     * new Đơn_Hàng (Requirement 8.3).
     *
     * <p>Idempotent: deactivating a code that is already inactive simply
     * leaves it inactive and returns normally, rather than erroring, so
     * Admin retries/double-clicks don't fail.
     *
     * @throws ResourceNotFoundException if no discount code with the given
     *                                    id exists
     */
    @Transactional
    public DiscountCode deactivate(UUID id) {
        DiscountCode discountCode = discountCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DISCOUNT_CODE_NOT_FOUND_ERROR_CODE,
                        "Không tìm thấy Mã_Giảm_Giá."));

        discountCode.setActive(false);
        return discountCodeRepository.save(discountCode);
    }

    /**
     * Validates that a {@link DiscountCode} is currently eligible to be
     * applied to an order: it must exist, be {@code active}, have a
     * current time within {@code [validFrom, validTo]}, and have
     * {@code usageCount < maxUsage} (Requirement 8.3, 8.4).
     *
     * @throws ResourceNotFoundException if no discount code with the given
     *                                    code exists
     * @throws BusinessRuleException     if the code is inactive, not yet
     *                                    valid, expired, or has reached its
     *                                    usage limit
     */
    @Transactional(readOnly = true)
    public DiscountCode validate(String code) {
        DiscountCode discountCode = discountCodeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(DISCOUNT_CODE_NOT_FOUND_ERROR_CODE,
                        "Không tìm thấy Mã_Giảm_Giá."));

        if (!discountCode.isActive()) {
            throw new BusinessRuleException(DISCOUNT_CODE_INACTIVE_ERROR_CODE,
                    "Mã_Giảm_Giá đã bị vô hiệu hóa.");
        }

        Instant now = Instant.now();
        if (now.isBefore(discountCode.getValidFrom())) {
            throw new BusinessRuleException(DISCOUNT_CODE_NOT_YET_VALID_ERROR_CODE,
                    "Mã_Giảm_Giá chưa đến thời điểm có hiệu lực.");
        }
        if (now.isAfter(discountCode.getValidTo())) {
            throw new BusinessRuleException(DISCOUNT_CODE_EXPIRED_ERROR_CODE,
                    "Mã_Giảm_Giá đã hết hạn sử dụng.");
        }

        if (discountCode.getUsageCount() >= discountCode.getMaxUsage()) {
            throw new BusinessRuleException(DISCOUNT_CODE_USAGE_LIMIT_REACHED_ERROR_CODE,
                    "Mã_Giảm_Giá đã đạt số lần sử dụng tối đa.");
        }

        return discountCode;
    }
}
