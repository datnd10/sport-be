package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.discount.DiscountCodeCreateRequest;
import com.example.sport_be.dto.discount.DiscountCodeResponse;
import com.example.sport_be.dto.discount.DiscountValidationResponse;
import com.example.sport_be.dto.discount.AvailableDiscountResponse;
import java.util.List;
import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.service.DiscountService;
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

import java.util.UUID;

/**
 * Endpoints for managing Mã_Giảm_Giá (Discount_Code) records
 * (Requirement 8).
 *
 * <p>Routes under {@code /api/admin/**} require {@code ROLE_ADMIN}, per
 * {@code SecurityConfig}.
 */
@RestController
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    /** Available checkout offers for authenticated customers. Validation is repeated on application/order creation. */
    @GetMapping(ApiPaths.Discount.AVAILABLE)
    public ResponseEntity<List<AvailableDiscountResponse>> availableDiscountCodes() {
        return ResponseEntity.ok(discountService.findAvailable());
    }

    /**
     * Returns a page of Mã_Giảm_Giá for the Admin management page
     * (Requirement 8.1, 8.3), most recently valid-from first, optionally
     * filtered by a case-insensitive {@code code} substring.
     */
    @GetMapping(ApiPaths.Discount.ADMIN_LIST)
    public ResponseEntity<PageResponse<DiscountCodeResponse>> getAllDiscountCodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(discountService.findAllPaged(page, size, keyword));
    }

    /**
     * Creates a new Mã_Giảm_Giá, rejecting duplicate codes (Requirement
     * 8.1, 8.2).
     */
    @PostMapping(ApiPaths.Discount.ADMIN_CREATE)
    public ResponseEntity<DiscountCodeResponse> createDiscountCode(@Valid @RequestBody DiscountCodeCreateRequest request) {
        DiscountCode discountCode = discountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DiscountCodeResponse.from(discountCode));
    }

    /**
     * Deactivates a Mã_Giảm_Giá so it can no longer be applied to new
     * Đơn_Hàng (Requirement 8.3).
     */
    @PutMapping(ApiPaths.Discount.ADMIN_DEACTIVATE)
    public ResponseEntity<DiscountCodeResponse> deactivateDiscountCode(@PathVariable UUID id) {
        DiscountCode discountCode = discountService.deactivate(id);
        return ResponseEntity.ok(DiscountCodeResponse.from(discountCode));
    }

    /**
     * Checks whether a Mã_Giảm_Giá is currently eligible to be applied to
     * an order: active, within its validity window, and under its usage
     * limit (Requirement 8.3, 8.4). Public; used by Customers/Guests
     * during checkout.
     */
    @GetMapping(ApiPaths.Discount.PUBLIC_VALIDATE)
    public ResponseEntity<DiscountValidationResponse> validateDiscountCode(@RequestParam String code) {
        DiscountCode discountCode = discountService.validate(code);
        return ResponseEntity.ok(DiscountValidationResponse.from(discountCode));
    }
}
