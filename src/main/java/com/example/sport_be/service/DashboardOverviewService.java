package com.example.sport_be.service;

import com.example.sport_be.dto.dashboard.*;
import com.example.sport_be.dto.dashboard.DashboardOverviewResponse.*;
import com.example.sport_be.dto.order.OrderSummaryResponse;
import com.example.sport_be.entity.Order;
import com.example.sport_be.entity.OrderItem;
import com.example.sport_be.enums.*;
import com.example.sport_be.exception.ValidationException;
import com.example.sport_be.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** A coherent dashboard snapshot. All period metrics use order creation dates in Vietnam. */
@Service
public class DashboardOverviewService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductVariantRepository variants;
    private final AccountRepository accounts;

    public DashboardOverviewService(OrderRepository orders, OrderItemRepository items,
            ProductVariantRepository variants, AccountRepository accounts) {
        this.orders = orders; this.items = items; this.variants = variants; this.accounts = accounts;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardOverviewResponse get(String range, LocalDate requestedFrom, LocalDate requestedTo) {
        Instant now = Instant.now();
        LocalDate today = now.atZone(ZONE).toLocalDate();
        LocalDate to = today;
        LocalDate from;
        switch (range) {
            case "today" -> from = today;
            case "7days" -> from = today.minusDays(6);
            case "30days" -> from = today.minusDays(29);
            case "month" -> from = today.withDayOfMonth(1);
            case "custom" -> { from = requestedFrom; to = requestedTo; }
            default -> throw invalid("Khoảng thời gian không hợp lệ.");
        }
        if (from == null || to == null || from.isAfter(to) || to.isAfter(today)
                || ChronoUnit.DAYS.between(from, to) >= 366) {
            throw invalid("Chọn khoảng ngày hợp lệ, tối đa 366 ngày và không vượt quá hôm nay.");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate previousTo = from.minusDays(1), previousFrom = from.minusDays(days);
        var current = periodOrders(from, to);
        var previous = periodOrders(previousFrom, previousTo);
        var currentItems = current.isEmpty() ? List.<OrderItem>of()
                : items.findByOrder_IdIn(current.stream().map(Order::getId).toList());
        var previousItems = previous.isEmpty() ? List.<OrderItem>of()
                : items.findByOrder_IdIn(previous.stream().map(Order::getId).toList());
        var breakdown = sales(current, currentItems);
        List<Trend> trend = new ArrayList<>();
        Map<LocalDate, List<Order>> byDay = current.stream().collect(Collectors.groupingBy(o -> o.getCreatedAt().atZone(ZONE).toLocalDate()));
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            var daily = byDay.getOrDefault(date, List.of());
            trend.add(new Trend(date, value(daily, false), value(daily, true)));
        }
        var alerts = variants.findStockAlerts(PageRequest.of(0, 8)).stream().map(v -> new StockAlert(
                v.getId(), v.getProduct().getId(), v.getProduct().getName(), v.getSku(), v.getStockQuantity(),
                v.getLowStockThreshold(), StockStatus.of(v).name())).toList();
        var operations = new Operations(orders.countByStatus(OrderStatus.PENDING), orders.countByStatus(OrderStatus.CONFIRMED),
                orders.countByStatus(OrderStatus.SHIPPING), variants.countLowStockActiveVariants(), variants.countOutOfStockActiveVariants());
        var recent = orders.findAll(PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .getContent().stream().map(OrderSummaryResponse::from).toList();
        return new DashboardOverviewResponse(from, to, previousFrom, previousTo, ZONE.getId(), now,
                metrics(current, currentItems, from, to), metrics(previous, previousItems, previousFrom, previousTo),
                trend, breakdown.categories(), breakdown.products(), operations, recent, alerts);
    }

    private ValidationException invalid(String message) {
        return new ValidationException("INVALID_DASHBOARD_RANGE", message, List.of());
    }

    private List<Order> periodOrders(LocalDate from, LocalDate to) {
        return orders.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                from.atStartOfDay(ZONE).toInstant(), to.plusDays(1).atStartOfDay(ZONE).toInstant());
    }

    private BigDecimal value(List<Order> list, boolean deliveredOnly) {
        return list.stream().filter(o -> deliveredOnly ? o.getStatus() == OrderStatus.DELIVERED : o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Metrics metrics(List<Order> list, List<OrderItem> lines, LocalDate from, LocalDate to) {
        var valid = list.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();
        var ids = valid.stream().map(Order::getId).collect(Collectors.toSet());
        var ordered = value(list, false);
        var statuses = Arrays.stream(OrderStatus.values()).map(s -> new OrderStatusBreakdownResponse(s,
                list.stream().filter(o -> o.getStatus() == s).count())).toList();
        return new Metrics(list.size(), ordered, value(list, true),
                valid.stream().map(Order::getDiscountAmount).reduce(BigDecimal.ZERO, BigDecimal::add),
                valid.isEmpty() ? BigDecimal.ZERO : ordered.divide(BigDecimal.valueOf(valid.size()), 2, RoundingMode.HALF_UP),
                accounts.countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Role.CUSTOMER,
                        from.atStartOfDay(ZONE).toInstant(), to.plusDays(1).atStartOfDay(ZONE).toInstant()),
                list.size() - valid.size(), list.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count(),
                lines.stream().filter(i -> ids.contains(i.getOrder().getId())).mapToLong(OrderItem::getQuantity).sum(), statuses);
    }

    private record Sales(List<CategoryRevenueResponse> categories, List<ProductSales> products) {}

    /** Allocate each order's net total proportionally, assigning rounding residue to its last line. */
    private Sales sales(List<Order> list, List<OrderItem> lines) {
        var byOrder = lines.stream().collect(Collectors.groupingBy(i -> i.getOrder().getId()));
        Map<UUID, BigDecimal> categoryValues = new HashMap<>(), productValues = new HashMap<>();
        Map<UUID, String> categoryNames = new HashMap<>(), productNames = new HashMap<>();
        Map<UUID, Long> quantities = new HashMap<>();
        for (var order : list) {
            if (order.getStatus() == OrderStatus.CANCELLED) continue;
            var orderLines = byOrder.getOrDefault(order.getId(), List.of()).stream().sorted(Comparator.comparing(OrderItem::getId)).toList();
            BigDecimal gross = orderLines.stream().map(i -> i.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = order.getTotalAmount();
            for (int index = 0; index < orderLines.size(); index++) {
                var item = orderLines.get(index);
                BigDecimal part = index == orderLines.size() - 1 ? remaining : gross.signum() == 0 ? BigDecimal.ZERO
                        : order.getTotalAmount().multiply(item.getUnitPriceSnapshot()).multiply(BigDecimal.valueOf(item.getQuantity()))
                        .divide(gross, 2, RoundingMode.HALF_UP).min(remaining).max(BigDecimal.ZERO);
                remaining = remaining.subtract(part);
                var product = item.getVariant().getProduct();
                var category = product.getCategory();
                categoryValues.merge(category.getId(), part, BigDecimal::add);
                categoryNames.put(category.getId(), category.getName());
                productValues.merge(product.getId(), part, BigDecimal::add);
                productNames.put(product.getId(), product.getName());
                quantities.merge(product.getId(), (long) item.getQuantity(), Long::sum);
            }
        }
        var categories = categoryValues.entrySet().stream()
                .map(e -> new CategoryRevenueResponse(e.getKey(), categoryNames.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(CategoryRevenueResponse::revenue).reversed().thenComparing(CategoryRevenueResponse::categoryName)).toList();
        var products = quantities.entrySet().stream().map(e -> new ProductSales(e.getKey(), productNames.get(e.getKey()), e.getValue(), productValues.get(e.getKey())))
                .sorted(Comparator.comparingLong(ProductSales::quantity).reversed().thenComparing(ProductSales::productName)).limit(10).toList();
        return new Sales(categories, products);
    }
}
