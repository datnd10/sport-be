package com.example.sport_be.service;

import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.order.OrderSummaryResponse;
import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.exception.ForbiddenException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.OrderRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
 * Read-only queries over {@link Order} records for the order history and
 * order management views (Requirement 5.1, 5.2, 5.6).
 *
 * <p>Enforces that a Customer may only ever see their own orders: the
 * list returned by {@link #getOrdersForAccount} is scoped to the calling
 * Account (5.1), and {@link #getOrderDetail} rejects any Customer
 * request for an order they do not own (5.6). An Admin caller bypasses
 * the ownership check and may view any order.
 */
@Service
public class OrderQueryService {

    private static final String ORDER_NOT_FOUND_MESSAGE = "Không tìm thấy Đơn_Hàng.";
    private static final String FORBIDDEN_MESSAGE = "Bạn không có quyền xem Đơn_Hàng này.";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderQueryService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * Returns the order history for a single Account, most recent first:
     * only Đơn_Hàng belonging to {@code accountId}, never another
     * Customer's orders (Requirement 5.1).
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersForAccount(UUID accountId) {
        return orderRepository.findByAccount_Id(accountId, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Returns the full list of every Đơn_Hàng in the system, most recent
     * first, for the Admin order management view (Requirement 5.2).
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Default ordering applied to the Admin Đơn_Hàng management list:
     * createdAt descending (newest first), matching the previous
     * unpaginated behavior of {@link #getAllOrders()}.
     */
    private static final Sort ADMIN_LIST_DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    /**
     * Returns a paginated, optionally keyword- and status-filtered page
     * of Đơn_Hàng, most recent first, for the Admin order management
     * view (Requirement 5.2).
     *
     * <p>{@code keyword} performs a case-insensitive, partial-match
     * search against the order code or, for orders placed by a
     * registered Account, the customer's name/email (joining
     * {@code account}); guest orders (where {@code account} is
     * {@code null}) are only matched by order code. {@code status}
     * restricts results to Đơn_Hàng with that exact
     * Trạng_Thái_Đơn_Hàng. Both filters are optional and may be combined,
     * applied at the database level via {@link Specification} so the
     * full table is never loaded into memory.
     *
     * @param page    0-based page index
     * @param size    page size
     * @param keyword optional case-insensitive, partial-match filter on
     *                order code or customer name/email
     * @param status  optional exact-match filter on Trạng_Thái_Đơn_Hàng
     */
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getAllOrdersPaged(int page, int size, String keyword, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size, ADMIN_LIST_DEFAULT_SORT);
        Specification<Order> spec = buildAdminListSpecification(keyword, status);

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        return PageResponse.from(orderPage, OrderSummaryResponse::from);
    }

    private Specification<Order> buildAdminListSpecification(String keyword, OrderStatus status) {
        Specification<Order> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> {
                // LEFT join so guest orders (account == null) are not excluded entirely;
                // LOWER(NULL) LIKE pattern evaluates to NULL (not true), so a guest order
                // only ever matches via orderCode, never via a null account field.
                Join<Order, Account> accountJoin = root.join("account", JoinType.LEFT);
                return cb.or(
                        cb.like(cb.lower(root.get("orderCode")), pattern),
                        cb.like(cb.lower(accountJoin.get("fullName")), pattern),
                        cb.like(cb.lower(accountJoin.get("email")), pattern)
                );
            });
        }

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return spec;
    }

    /**
     * Returns a single Đơn_Hàng's full detail together with its line
     * items, for the order detail view (Requirement 5.6).
     *
     * <p>Access is restricted by caller: when {@code requesterIsAdmin} is
     * {@code false}, the order must belong to {@code requesterAccountId}
     * or the request is rejected. An Admin caller may view any order
     * regardless of {@code requesterAccountId}.
     *
     * @param orderId           the order to look up
     * @param requesterAccountId the calling Account's id, or {@code null} for an unauthenticated/guest caller
     * @param requesterIsAdmin  whether the caller has the Admin role
     * @throws ResourceNotFoundException if no order with {@code orderId} exists
     * @throws ForbiddenException        if a non-Admin caller does not own the order
     */
    @Transactional(readOnly = true)
    public OrderDetail getOrderDetail(UUID orderId, UUID requesterAccountId, boolean requesterIsAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));

        if (!requesterIsAdmin && !isOwnedBy(order, requesterAccountId)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }

        List<OrderItem> items = orderItemRepository.findByOrder_Id(orderId);
        return new OrderDetail(order, items);
    }

    private boolean isOwnedBy(Order order, UUID requesterAccountId) {
        return requesterAccountId != null
                && order.getAccount() != null
                && requesterAccountId.equals(order.getAccount().getId());
    }

    /**
     * Pairs an {@link Order} with its {@link OrderItem} list, as needed
     * to build the order detail response (Requirement 5.6).
     */
    public record OrderDetail(Order order, List<OrderItem> items) {
    }
}
