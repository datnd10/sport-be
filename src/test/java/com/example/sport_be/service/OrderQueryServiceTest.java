package com.example.sport_be.service;

import com.example.sport_be.dto.common.PageResponse;
import com.example.sport_be.dto.order.OrderSummaryResponse;
import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.Order;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.enums.PaymentMethod;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OrderQueryService#getAllOrdersPaged}, covering
 * pagination, keyword search, and status filtering for the Admin
 * Đơn_Hàng management list (Requirement 5.2).
 */
@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    private OrderQueryService orderQueryService;

    @BeforeEach
    void setUp() {
        orderQueryService = new OrderQueryService(orderRepository, orderItemRepository);
    }

    private Order newOrder() {
        Account account = new Account("customer@example.com", "hash", Role.CUSTOMER);
        return new Order(account, PaymentMethod.COD, BigDecimal.TEN, BigDecimal.TEN,
                "Nguyen Van A", "0900000000", "123 Đường ABC");
    }

    @Test
    void getAllOrdersPaged_withoutFilters_returnsPageResponseSortedByCreatedAtDesc() {
        Order order = newOrder();
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> repositoryPage = new PageImpl<>(List.of(order), expectedPageable, 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<OrderSummaryResponse> response = orderQueryService.getAllOrdersPaged(0, 20, null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void getAllOrdersPaged_withKeyword_delegatesFilteringToRepositorySpecification() {
        Order order = newOrder();
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> repositoryPage = new PageImpl<>(List.of(order), pageable, 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<OrderSummaryResponse> response = orderQueryService.getAllOrdersPaged(0, 20, order.getOrderCode(), null);

        assertThat(response.content()).hasSize(1);
        verify(orderRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllOrdersPaged_withStatus_delegatesFilteringToRepositorySpecification() {
        Order order = newOrder();
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> repositoryPage = new PageImpl<>(List.of(order), pageable, 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<OrderSummaryResponse> response = orderQueryService.getAllOrdersPaged(0, 20, null, OrderStatus.PENDING);

        assertThat(response.content()).hasSize(1);
        verify(orderRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllOrdersPaged_withSmallPageSize_buildsPageableWithRequestedPaging() {
        Order order = newOrder();
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> repositoryPage = new PageImpl<>(List.of(order), pageable, 5);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(repositoryPage);

        PageResponse<OrderSummaryResponse> response = orderQueryService.getAllOrdersPaged(1, 2, null, null);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
    }
}
