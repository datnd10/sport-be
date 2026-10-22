package com.example.sport_be.controller;

import com.example.sport_be.constants.ApiPaths;
import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.order.CreateOrderRequest;
import com.example.sport_be.dto.order.OrderResponse;
import com.example.sport_be.dto.order.OrderSummaryResponse;
import com.example.sport_be.dto.order.UpdateOrderStatusRequest;
import com.example.sport_be.entity.Order;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.security.JwtPrincipal;
import com.example.sport_be.service.OrderQueryService;
import com.example.sport_be.service.OrderService;
import com.example.sport_be.service.OrderStatusService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
 * Endpoints for placing and managing Đơn_Hàng (Order) records
 * (Requirement 4, 5).
 *
 * <p>{@code POST /api/orders} is available to both authenticated
 * Customers and guests, per design.md's endpoint table; it is listed as
 * a public route in {@code SecurityConfig} so unauthenticated requests
 * are allowed through as a guest checkout.
 *
 * <p>{@code GET /api/orders}, {@code GET /api/orders/{id}}, and
 * {@code GET /api/admin/orders} require an authenticated caller: the
 * first two fall under {@code anyRequest().authenticated()} in {@code
 * SecurityConfig} (available to Customer or Admin), while the admin
 * list is restricted to {@code ROLE_ADMIN} via the {@code /api/admin/**}
 * matcher.
 */
@RestController
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    private final OrderStatusService orderStatusService;
    private final OrderItemRepository orderItemRepository;

    public OrderController(OrderService orderService, OrderQueryService orderQueryService,
                            OrderStatusService orderStatusService, OrderItemRepository orderItemRepository) {
        this.orderService = orderService;
        this.orderQueryService = orderQueryService;
        this.orderStatusService = orderStatusService;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * Places a new Đơn_Hàng from the submitted line items and shipping
     * info (Requirement 4.1, 4.2, 4.3, 4.6, 4.7). If the caller is an
     * authenticated Customer, the order is associated with their
     * Account; otherwise it is placed as a guest order.
     */
    @PostMapping(ApiPaths.Order.BASE)
    public ResponseEntity<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        UUID accountId = currentAccountIdOrNull(authentication);
        OrderService.OrderCreationResult result = orderService.createOrder(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(OrderResponse.from(result.order(), result.items(), result.bankTransferInfo()));
    }

    /**
     * Returns the order history for the currently authenticated Customer:
     * only Đơn_Hàng belonging to that Account, never another Customer's
     * orders (Requirement 5.1).
     */
    @GetMapping(ApiPaths.Order.BASE)
    public ResponseEntity<List<OrderSummaryResponse>> listMyOrders(Authentication authentication) {
        UUID accountId = currentAccountId(authentication);
        List<OrderSummaryResponse> orders = orderQueryService.getOrdersForAccount(accountId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    /**
     * Returns a single Đơn_Hàng's full detail: purchased line items,
     * quantities, prices, payment method, and shipping address
     * (Requirement 5.6). A Customer may only view their own order; an
     * Admin may view any order.
     */
    @GetMapping(ApiPaths.Order.BY_ID)
    public ResponseEntity<OrderResponse> getOrderDetail(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID accountId = currentAccountIdOrNull(authentication);
        boolean isAdmin = isAdmin(authentication);
        OrderQueryService.OrderDetail detail = orderQueryService.getOrderDetail(id, accountId, isAdmin);
        return ResponseEntity.ok(OrderResponse.from(detail.order(), detail.items()));
    }

    /**
     * Returns a paginated, optionally filtered page of every Đơn_Hàng in
     * the system, including customer info, total amount, and order
     * status, for the Admin order management view (Requirement 5.2).
     *
     * <p>{@code keyword} performs a case-insensitive, partial-match
     * search against the order code or customer name/email.
     * {@code status} restricts results to a single
     * Trạng_Thái_Đơn_Hàng; an unrecognized value is silently ignored
     * (the filter is simply not applied) rather than failing the
     * request, consistent with {@code ProductService}'s handling of an
     * invalid {@code sortBy}.
     *
     * @param page    0-based page index (default 0)
     * @param size    page size (default 20)
     * @param keyword optional order code / customer name/email search
     * @param status  optional Trạng_Thái_Đơn_Hàng filter (e.g.
     *                {@code PENDING}, {@code CONFIRMED}, ...); invalid
     *                values are ignored
     */
    @GetMapping(ApiPaths.Order.ADMIN_LIST)
    public ResponseEntity<PageResponse<OrderSummaryResponse>> listAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        OrderStatus orderStatus = parseStatusOrNull(status);
        return ResponseEntity.ok(orderQueryService.getAllOrdersPaged(page, size, keyword, orderStatus));
    }

    private OrderStatus parseStatusOrNull(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Updates an Đơn_Hàng's Trạng_Thái_Đơn_Hàng to the submitted target
     * status, only when that transition is allowed by the order status
     * state machine (Requirement 5.3, 5.5). Restricted to {@code
     * ROLE_ADMIN} via the {@code /api/admin/**} matcher in {@code
     * SecurityConfig}.
     */
    @PutMapping(ApiPaths.Order.ADMIN_STATUS_BY_ID)
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        Order order = orderStatusService.transition(id, request.status());
        return ResponseEntity.ok(OrderResponse.from(order, orderItemRepository.findByOrder_Id(order.getId())));
    }

    private UUID currentAccountIdOrNull(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtPrincipal principal)) {
            return null;
        }
        return principal.accountId();
    }

    private UUID currentAccountId(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return principal.accountId();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof JwtPrincipal principal
                && principal.role() == Role.ADMIN;
    }
}
