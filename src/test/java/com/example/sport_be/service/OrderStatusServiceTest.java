package com.example.sport_be.service;

import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.enums.PaymentMethod;
import com.example.sport_be.exception.BusinessRuleException;
import com.example.sport_be.exception.ResourceNotFoundException;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.OrderRepository;
import com.example.sport_be.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderStatusService#transition} covering the
 * Trạng_Thái_Đơn_Hàng finite state machine (Requirement 5.3, 5.5).
 */
@ExtendWith(MockitoExtension.class)
class OrderStatusServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private InventoryService inventoryService;

    private OrderStatusService orderStatusService;

    private Order order;

    @BeforeEach
    void setUp() {
        orderStatusService = new OrderStatusService(orderRepository, orderStatusHistoryRepository,
                orderItemRepository, inventoryService);
        order = new Order(null, PaymentMethod.COD, BigDecimal.TEN, BigDecimal.TEN,
                "Nguyễn Văn A", "0900000000", "123 Đường ABC");
    }

    @Test
    void transition_fromPendingToConfirmed_succeedsAndRecordsHistory() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderStatusService.transition(UUID.randomUUID(), OrderStatus.CONFIRMED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderStatusHistoryRepository).save(any());
    }

    @Test
    void transition_fromConfirmedToShipping_succeeds() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderStatusService.transition(UUID.randomUUID(), OrderStatus.SHIPPING);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPING);
    }

    @Test
    void transition_fromShippingToDelivered_succeeds() {
        order.setStatus(OrderStatus.SHIPPING);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderStatusService.transition(UUID.randomUUID(), OrderStatus.DELIVERED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void transition_fromPendingToCancelled_succeeds() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrder_Id(any())).thenReturn(List.of());

        Order result = orderStatusService.transition(UUID.randomUUID(), OrderStatus.CANCELLED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void transition_fromConfirmedToCancelled_succeeds() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrder_Id(any())).thenReturn(List.of());

        Order result = orderStatusService.transition(UUID.randomUUID(), OrderStatus.CANCELLED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void transition_toCancelled_restoresInventoryForEveryOrderItem() {
        // Requirement 5.4: cancelling an order restores the Tồn_Kho deducted for each item.
        order.setStatus(OrderStatus.CONFIRMED);
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariant variantA = mock(ProductVariant.class);
        UUID variantAId = UUID.randomUUID();
        when(variantA.getId()).thenReturn(variantAId);

        ProductVariant variantB = mock(ProductVariant.class);
        UUID variantBId = UUID.randomUUID();
        when(variantB.getId()).thenReturn(variantBId);

        OrderItem itemA = new OrderItem(order, variantA, "Áo thể thao", BigDecimal.TEN, 2);
        OrderItem itemB = new OrderItem(order, variantB, "Quần thể thao", BigDecimal.TEN, 5);
        // Order.id is generated by the database and is null on this not-yet-persisted
        // instance, so the service looks items up by whatever id the saved order carries.
        when(orderItemRepository.findByOrder_Id(order.getId())).thenReturn(List.of(itemA, itemB));

        orderStatusService.transition(orderId, OrderStatus.CANCELLED);

        verify(inventoryService).restoreForCancellation(variantAId, 2);
        verify(inventoryService).restoreForCancellation(variantBId, 5);
    }

    @Test
    void transition_toNonCancelledStatus_neverTouchesInventory() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderStatusService.transition(UUID.randomUUID(), OrderStatus.CONFIRMED);

        verify(orderItemRepository, never()).findByOrder_Id(any());
        verify(inventoryService, never()).restoreForCancellation(any(), anyInt());
    }

    @Test
    void transition_fromPendingToShipping_rejectsAsInvalidTransition() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderStatusService.transition(UUID.randomUUID(), OrderStatus.SHIPPING))
                .isInstanceOf(BusinessRuleException.class);

        verify(orderRepository, never()).save(any());
        verify(orderStatusHistoryRepository, never()).save(any());
    }

    @Test
    void transition_fromPendingToDelivered_rejectsAsInvalidTransition() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderStatusService.transition(UUID.randomUUID(), OrderStatus.DELIVERED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED"})
    void transition_fromDelivered_isAlwaysRejectedAsTerminal(OrderStatus targetStatus) {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderStatusService.transition(UUID.randomUUID(), targetStatus))
                .isInstanceOf(BusinessRuleException.class);

        verify(orderRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED"})
    void transition_fromCancelled_isAlwaysRejectedAsTerminal(OrderStatus targetStatus) {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderStatusService.transition(UUID.randomUUID(), targetStatus))
                .isInstanceOf(BusinessRuleException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void transition_withUnknownOrderId_throwsResourceNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderStatusService.transition(orderId, OrderStatus.CONFIRMED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transition_rejectingDeliveredBackToPending_throwsBusinessRuleException() {
        // Requirement 5.5 explicit example: DELIVERED/CANCELLED back to PENDING must be rejected.
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderStatusService.transition(UUID.randomUUID(), OrderStatus.PENDING))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void isTransitionAllowed_returnsTrueOnlyForDefinedEdges() {
        assertThat(orderStatusService.isTransitionAllowed(OrderStatus.PENDING, OrderStatus.CONFIRMED)).isTrue();
        assertThat(orderStatusService.isTransitionAllowed(OrderStatus.PENDING, OrderStatus.CANCELLED)).isTrue();
        assertThat(orderStatusService.isTransitionAllowed(OrderStatus.CONFIRMED, OrderStatus.SHIPPING)).isTrue();
        assertThat(orderStatusService.isTransitionAllowed(OrderStatus.CONFIRMED, OrderStatus.CANCELLED)).isTrue();
        assertThat(orderStatusService.isTransitionAllowed(OrderStatus.SHIPPING, OrderStatus.DELIVERED)).isTrue();

        assertThat(orderStatusService.isTransitionAllowed(OrderStatus.PENDING, OrderStatus.DELIVERED)).isFalse();
        assertThat(orderStatusService.isTransitionAllowed(OrderStatus.SHIPPING, OrderStatus.CANCELLED)).isFalse();
        assertThat(orderStatusService.isTransitionAllowed(OrderStatus.DELIVERED, OrderStatus.PENDING)).isFalse();
        assertThat(orderStatusService.isTransitionAllowed(OrderStatus.CANCELLED, OrderStatus.CONFIRMED)).isFalse();
    }
}
