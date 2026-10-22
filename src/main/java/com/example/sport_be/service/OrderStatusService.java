package com.example.sport_be.service;

import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.entity.OrderStatusHistory;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.exception.BusinessRuleException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.OrderRepository;
import com.example.sport_be.repository.OrderStatusHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Enforces the Trạng_Thái_Đơn_Hàng (Order_Status) finite state machine
 * and records every successful transition in {@link OrderStatusHistory}
 * (Requirement 5.3, 5.5):
 *
 * <pre>
 * PENDING   -&gt; CONFIRMED
 * PENDING   -&gt; CANCELLED
 * CONFIRMED -&gt; SHIPPING
 * CONFIRMED -&gt; CANCELLED
 * SHIPPING  -&gt; DELIVERED
 * </pre>
 *
 * <p>{@code DELIVERED} and {@code CANCELLED} are terminal statuses: any
 * transition attempted from either of them is rejected (Requirement
 * 5.5), as is any {@code (from, to)} pair not listed above.
 *
 * <p>When a transition's target status is {@code CANCELLED}, this
 * service also restores Tồn_Kho for every {@link OrderItem} in the
 * order by delegating to {@link InventoryService#restoreForCancellation}
 * (Requirement 5.4), within the same transaction as the status update
 * so the status change and stock restoration are atomic.
 */
@Service
public class OrderStatusService {

    public static final String INVALID_TRANSITION_ERROR_CODE = "INVALID_ORDER_STATUS_TRANSITION";
    private static final String ORDER_NOT_FOUND_MESSAGE = "Không tìm thấy Đơn_Hàng.";

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = buildAllowedTransitions();

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;

    public OrderStatusService(OrderRepository orderRepository,
                               OrderStatusHistoryRepository orderStatusHistoryRepository,
                               OrderItemRepository orderItemRepository,
                               InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Transitions {@code orderId}'s status to {@code targetStatus}, if
     * and only if that transition is allowed from its current status
     * (Requirement 5.3). On success, persists the new status and
     * appends an {@link OrderStatusHistory} entry recording the
     * {@code fromStatus}/{@code toStatus} pair.
     *
     * <p>If {@code targetStatus} is {@code CANCELLED}, every
     * {@link OrderItem} belonging to this order has its purchased
     * quantity restored to Tồn_Kho via
     * {@link InventoryService#restoreForCancellation} before the method
     * returns (Requirement 5.4). Because this method is
     * {@code @Transactional}, the status update and all stock
     * restorations commit or roll back together.
     *
     * @throws ResourceNotFoundException if no order with {@code orderId} exists
     * @throws BusinessRuleException     if the transition is not allowed, including any
     *                                    attempt to transition out of a terminal status
     *                                    ({@code DELIVERED} or {@code CANCELLED}) (Requirement 5.5)
     */
    @Transactional
    public Order transition(UUID orderId, OrderStatus targetStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(ORDER_NOT_FOUND_MESSAGE));

        OrderStatus currentStatus = order.getStatus();
        if (!isTransitionAllowed(currentStatus, targetStatus)) {
            throw new BusinessRuleException(INVALID_TRANSITION_ERROR_CODE,
                    "Không thể chuyển Trạng_Thái_Đơn_Hàng từ " + currentStatus + " sang " + targetStatus + ".");
        }

        order.setStatus(targetStatus);
        Order saved = orderRepository.save(order);

        orderStatusHistoryRepository.save(new OrderStatusHistory(saved, currentStatus.name(), targetStatus.name()));

        if (targetStatus == OrderStatus.CANCELLED) {
            restoreInventoryForCancellation(saved);
        }

        return saved;
    }

    /**
     * Restores Tồn_Kho for every {@link OrderItem} of a cancelled order
     * (Requirement 5.4), delegating each variant's quantity to
     * {@link InventoryService#restoreForCancellation}.
     */
    private void restoreInventoryForCancellation(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder_Id(order.getId());
        for (OrderItem item : items) {
            inventoryService.restoreForCancellation(item.getVariant().getId(), item.getQuantity());
        }
    }

    /**
     * Returns whether the state machine allows moving from
     * {@code currentStatus} directly to {@code targetStatus}.
     */
    public boolean isTransitionAllowed(OrderStatus currentStatus, OrderStatus targetStatus) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    private static Map<OrderStatus, Set<OrderStatus>> buildAllowedTransitions() {
        Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);
        transitions.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPING, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED));
        transitions.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        transitions.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        return transitions;
    }
}
