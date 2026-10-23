package com.example.sport_be.service;

import com.example.sport_be.dto.dashboard.CategoryRevenueResponse;
import com.example.sport_be.dto.dashboard.DailyRevenueResponse;
import com.example.sport_be.dto.dashboard.DashboardSummaryResponse;
import com.example.sport_be.dto.dashboard.OrderStatusBreakdownResponse;
import com.example.sport_be.dto.dashboard.TopProductResponse;
import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.DashboardRange;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.enums.Role;
import com.example.sport_be.enums.StockStatus;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.OrderItemRepository;
import com.example.sport_be.repository.OrderRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Aggregate queries backing the admin Dashboard overview (Requirement 10).
 *
 * <p>{@link #getSummary} computes, for a selected {@link DashboardRange}:
 * the total number of Đơn_Hàng created in that range (every status), the
 * total revenue for that same range excluding Đã_Hủy orders, and the
 * current count of low-stock Biến_Thể_Sản_Phẩm (Property 32). The
 * low-stock count intentionally ignores the time range: it is always a
 * snapshot of current stock levels, consistent with the stock status
 * derivation already used by {@code InventoryService}/{@link StockStatus}
 * (Requirement 7.3, 7.4).
 *
 * <p>{@link #getTopProducts} computes, for the same selectable ranges,
 * the list of best-selling Sản_Phẩm sorted descending by total quantity
 * sold, also excluding Đã_Hủy orders for consistency with the revenue
 * figure above (Requirement 10.3, Property 34).
 */
@Service
public class DashboardService {

    private static final String INVALID_RANGE_ERROR_CODE = "INVALID_DASHBOARD_RANGE";

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;
    private final AccountRepository accountRepository;
    private ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");

    public DashboardService(OrderRepository orderRepository, ProductVariantRepository productVariantRepository,
                             OrderItemRepository orderItemRepository, AccountRepository accountRepository) {
        this.orderRepository = orderRepository;
        this.productVariantRepository = productVariantRepository;
        this.orderItemRepository = orderItemRepository;
        this.accountRepository = accountRepository;
    }

    /** Overrides the time zone used to resolve ranges; intended for tests. */
    void setZoneId(ZoneId zoneId) {
        this.zoneId = zoneId;
    }

    /**
     * Computes the Dashboard summary for the given range key
     * ({@code today}, {@code 7days} or {@code month}).
     *
     * @throws ValidationException if {@code rangeKey} does not match a known range
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(String rangeKey) {
        DashboardRange range = parseRange(rangeKey);
        DashboardRange.Interval interval = range.resolve(zoneId, Instant.now());

        List<Order> ordersInRange = orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                interval.from(), interval.to());

        long totalOrders = ordersInRange.size();

        List<Order> nonCancelledOrders = ordersInRange.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .toList();

        BigDecimal totalRevenue = nonCancelledOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = nonCancelledOrders.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(nonCancelledOrders.size()), 2, RoundingMode.HALF_UP);

        long cancelledOrderCount = totalOrders - nonCancelledOrders.size();

        long lowStockProductCount = countLowStockVariants();

        long newCustomerCount = accountRepository.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                Role.CUSTOMER, interval.from(), interval.to());

        List<OrderStatusBreakdownResponse> statusBreakdown = buildStatusBreakdown(ordersInRange);

        return new DashboardSummaryResponse(
                totalOrders,
                totalRevenue,
                lowStockProductCount,
                averageOrderValue,
                newCustomerCount,
                cancelledOrderCount,
                statusBreakdown);
    }

    /**
     * Computes the revenue trend for the given range key, one point per
     * calendar day (including zero-revenue days) so the chart on the
     * admin Dashboard has no gaps (Requirement 10).
     *
     * @throws ValidationException if {@code rangeKey} does not match a known range
     */
    @Transactional(readOnly = true)
    public List<DailyRevenueResponse> getRevenueByDay(String rangeKey) {
        DashboardRange range = parseRange(rangeKey);
        DashboardRange.Interval interval = range.resolve(zoneId, Instant.now());

        List<Order> ordersInRange = orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                interval.from(), interval.to());

        Map<LocalDate, BigDecimal> revenueByDate = ordersInRange.stream()
                .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().atZone(zoneId).toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalAmount, BigDecimal::add)));

        LocalDate firstDay = interval.from().atZone(zoneId).toLocalDate();
        LocalDate lastDayInclusive = interval.to().atZone(zoneId).toLocalDate().minusDays(1);

        return Stream.iterate(firstDay, day -> !day.isAfter(lastDayInclusive), day -> day.plusDays(1))
                .map(day -> new DailyRevenueResponse(day, revenueByDate.getOrDefault(day, BigDecimal.ZERO)))
                .toList();
    }

    /**
     * Computes revenue per Danh_Mục for the given range key, sorted
     * descending by revenue (Requirement 10).
     *
     * @throws ValidationException if {@code rangeKey} does not match a known range
     */
    @Transactional(readOnly = true)
    public List<CategoryRevenueResponse> getRevenueByCategory(String rangeKey) {
        DashboardRange range = parseRange(rangeKey);
        DashboardRange.Interval interval = range.resolve(zoneId, Instant.now());

        List<Object[]> rows = orderItemRepository.sumRevenueByCategoryInRange(
                interval.from(), interval.to(), OrderStatus.CANCELLED);

        return rows.stream()
                .map(row -> new CategoryRevenueResponse(
                        (UUID) row[0],
                        (String) row[1],
                        (BigDecimal) row[2]))
                .toList();
    }

    private List<OrderStatusBreakdownResponse> buildStatusBreakdown(List<Order> ordersInRange) {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        for (Order order : ordersInRange) {
            counts.merge(order.getStatus(), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .map(entry -> new OrderStatusBreakdownResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(OrderStatusBreakdownResponse::status))
                .toList();
    }

    /**
     * Computes the best-selling Sản_Phẩm list for the given range key
     * ({@code today}, {@code 7days} or {@code month}), sorted descending
     * by total quantity sold (Requirement 10.3, Property 34).
     *
     * @throws ValidationException if {@code rangeKey} does not match a known range
     */
    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProducts(String rangeKey) {
        DashboardRange range = parseRange(rangeKey);
        DashboardRange.Interval interval = range.resolve(zoneId, Instant.now());

        List<Object[]> rows = orderItemRepository.sumQuantityByProductInRange(
                interval.from(), interval.to(), OrderStatus.CANCELLED);

        return rows.stream()
                .map(row -> new TopProductResponse(
                        (UUID) row[0],
                        (String) row[1],
                        (Long) row[2]))
                .toList();
    }

    private long countLowStockVariants() {
        List<ProductVariant> variants = productVariantRepository.findAll();
        return variants.stream()
                .filter(variant -> StockStatus.of(variant) == StockStatus.LOW_STOCK)
                .count();
    }

    private DashboardRange parseRange(String rangeKey) {
        try {
            return DashboardRange.fromKey(rangeKey);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(INVALID_RANGE_ERROR_CODE, e.getMessage(), Collections.emptyList());
        }
    }
}
