package com.example.sport_be.service;

import com.example.sport_be.dto.dashboard.CategoryRevenueResponse;
import com.example.sport_be.dto.dashboard.DailyRevenueResponse;
import com.example.sport_be.dto.dashboard.DashboardSummaryResponse;
import com.example.sport_be.dto.dashboard.OrderStatusBreakdownResponse;
import com.example.sport_be.dto.dashboard.TopProductResponse;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.enums.PaymentMethod;
import com.example.sport_be.enums.Role;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.OrderRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DashboardService#getSummary} (Requirement 10.1,
 * 10.2).
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private AccountRepository accountRepository;

    private DashboardService dashboardService;

    private Product product;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(orderRepository, productVariantRepository, orderItemRepository,
                accountRepository);
        dashboardService.setZoneId(ZoneId.of("UTC"));

        Category category = new Category("Vợt cầu lông", "vot-cau-long");
        product = new Product(category, "Vợt Yonex", BigDecimal.TEN);

        // Default stub shared by most getSummary tests; overridden where a
        // specific count matters.
        lenient().when(accountRepository.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(Role.CUSTOMER), any(), any())).thenReturn(0L);
        lenient().when(productVariantRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void getSummary_countsAllOrdersButExcludesCancelledFromRevenue() {
        Order confirmedOrder = newOrder(OrderStatus.CONFIRMED, BigDecimal.valueOf(100));
        Order cancelledOrder = newOrder(OrderStatus.CANCELLED, BigDecimal.valueOf(500));
        when(orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(List.of(confirmedOrder, cancelledOrder));

        DashboardSummaryResponse summary = dashboardService.getSummary("today");

        assertThat(summary.totalOrders()).isEqualTo(2);
        assertThat(summary.totalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void getSummary_computesAverageOrderValueExcludingCancelled() {
        Order first = newOrder(OrderStatus.CONFIRMED, BigDecimal.valueOf(100));
        Order second = newOrder(OrderStatus.DELIVERED, BigDecimal.valueOf(300));
        Order cancelled = newOrder(OrderStatus.CANCELLED, BigDecimal.valueOf(1000));
        when(orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(List.of(first, second, cancelled));

        DashboardSummaryResponse summary = dashboardService.getSummary("today");

        assertThat(summary.averageOrderValue()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(summary.cancelledOrderCount()).isEqualTo(1);
    }

    @Test
    void getSummary_averageOrderValueIsZeroWhenNoNonCancelledOrders() {
        Order cancelled = newOrder(OrderStatus.CANCELLED, BigDecimal.valueOf(1000));
        when(orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(List.of(cancelled));

        DashboardSummaryResponse summary = dashboardService.getSummary("today");

        assertThat(summary.averageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSummary_returnsStatusBreakdownSortedByStatus() {
        Order pending = newOrder(OrderStatus.PENDING, BigDecimal.valueOf(50));
        Order pending2 = newOrder(OrderStatus.PENDING, BigDecimal.valueOf(60));
        Order delivered = newOrder(OrderStatus.DELIVERED, BigDecimal.valueOf(70));
        when(orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(List.of(pending, pending2, delivered));

        DashboardSummaryResponse summary = dashboardService.getSummary("today");

        assertThat(summary.statusBreakdown()).containsExactly(
                new OrderStatusBreakdownResponse(OrderStatus.PENDING, 2L),
                new OrderStatusBreakdownResponse(OrderStatus.DELIVERED, 1L));
    }

    @Test
    void getSummary_returnsNewCustomerCountFromAccountRepository() {
        when(orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(List.of());
        when(accountRepository.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(Role.CUSTOMER), any(), any())).thenReturn(3L);

        DashboardSummaryResponse summary = dashboardService.getSummary("today");

        assertThat(summary.newCustomerCount()).isEqualTo(3L);
    }

    @Test
    void getSummary_countsOnlyLowStockVariants() {
        when(orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(List.of());

        ProductVariant outOfStock = new ProductVariant(product, "SKU-OUT", "size:S", BigDecimal.TEN, 0);
        ProductVariant lowStock = new ProductVariant(product, "SKU-LOW", "size:M", BigDecimal.TEN, 2);
        lowStock.setLowStockThreshold(5);
        ProductVariant inStock = new ProductVariant(product, "SKU-IN", "size:L", BigDecimal.TEN, 20);
        inStock.setLowStockThreshold(5);
        when(productVariantRepository.findAll()).thenReturn(List.of(outOfStock, lowStock, inStock));

        DashboardSummaryResponse summary = dashboardService.getSummary("7days");

        assertThat(summary.lowStockProductCount()).isEqualTo(1);
    }

    @Test
    void getSummary_rejectsUnknownRange() {
        assertThatThrownBy(() -> dashboardService.getSummary("invalid"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getRevenueByDay_returnsOnePointPerDayIncludingZeroRevenueDays() {
        ZoneId utc = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(utc);

        Order todayOrder = newOrder(OrderStatus.CONFIRMED, BigDecimal.valueOf(100));
        setCreatedAt(todayOrder, today.atStartOfDay(utc).toInstant().plusSeconds(3600));

        Order sixDaysAgoOrder = newOrder(OrderStatus.CONFIRMED, BigDecimal.valueOf(50));
        setCreatedAt(sixDaysAgoOrder, today.minusDays(6).atStartOfDay(utc).toInstant());

        when(orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
                .thenReturn(List.of(todayOrder, sixDaysAgoOrder));

        List<DailyRevenueResponse> revenueByDay = dashboardService.getRevenueByDay("7days");

        assertThat(revenueByDay).hasSize(7);
        assertThat(revenueByDay.get(0).date()).isEqualTo(today.minusDays(6));
        assertThat(revenueByDay.get(0).revenue()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(revenueByDay.get(6).date()).isEqualTo(today);
        assertThat(revenueByDay.get(6).revenue()).isEqualByComparingTo(BigDecimal.valueOf(100));
        // Days in between have no orders and should be zero-filled, not omitted.
        assertThat(revenueByDay.get(3).revenue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getRevenueByDay_rejectsUnknownRange() {
        assertThatThrownBy(() -> dashboardService.getRevenueByDay("invalid"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getRevenueByCategory_returnsRepositoryRowsInDescendingOrder() {
        UUID topCategoryId = UUID.randomUUID();
        UUID secondCategoryId = UUID.randomUUID();
        when(orderItemRepository.sumRevenueByCategoryInRange(any(), any(), eq(OrderStatus.CANCELLED)))
                .thenReturn(List.of(
                        new Object[]{topCategoryId, "Vợt cầu lông", BigDecimal.valueOf(500)},
                        new Object[]{secondCategoryId, "Giày thể thao", BigDecimal.valueOf(200)}
                ));

        List<CategoryRevenueResponse> revenueByCategory = dashboardService.getRevenueByCategory("month");

        assertThat(revenueByCategory).containsExactly(
                new CategoryRevenueResponse(topCategoryId, "Vợt cầu lông", BigDecimal.valueOf(500)),
                new CategoryRevenueResponse(secondCategoryId, "Giày thể thao", BigDecimal.valueOf(200)));
    }

    @Test
    void getRevenueByCategory_rejectsUnknownRange() {
        assertThatThrownBy(() -> dashboardService.getRevenueByCategory("invalid"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void getTopProducts_returnsRepositoryRowsInDescendingOrder() {
        UUID topProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        when(orderItemRepository.sumQuantityByProductInRange(any(), any(), eq(OrderStatus.CANCELLED)))
                .thenReturn(List.of(
                        new Object[]{topProductId, "Vợt Yonex", 15L},
                        new Object[]{secondProductId, "Giày Nike", 7L}
                ));

        List<TopProductResponse> topProducts = dashboardService.getTopProducts("month");

        assertThat(topProducts).containsExactly(
                new TopProductResponse(topProductId, "Vợt Yonex", 15L),
                new TopProductResponse(secondProductId, "Giày Nike", 7L)
        );
    }

    @Test
    void getTopProducts_rejectsUnknownRange() {
        assertThatThrownBy(() -> dashboardService.getTopProducts("invalid"))
                .isInstanceOf(ValidationException.class);
    }

    private Order newOrder(OrderStatus status, BigDecimal totalAmount) {
        Order order = new Order(null, PaymentMethod.COD, totalAmount, totalAmount,
                "Nguyen Van A", "0900000000", "123 Đường ABC");
        order.setStatus(status);
        return order;
    }

    /**
     * {@code Order.createdAt} is normally populated by Hibernate's
     * {@code @CreationTimestamp} at persist time and has no public
     * setter, so tests that need a specific value (e.g. for
     * {@code getRevenueByDay}'s per-day grouping) set it via reflection.
     */
    private void setCreatedAt(Order order, Instant createdAt) {
        ReflectionTestUtils.setField(order, "createdAt", createdAt);
    }
}
