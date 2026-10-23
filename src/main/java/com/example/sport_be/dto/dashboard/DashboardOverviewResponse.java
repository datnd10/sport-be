package com.example.sport_be.dto.dashboard;

import com.example.sport_be.dto.order.OrderSummaryResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardOverviewResponse(LocalDate from, LocalDate to, LocalDate previousFrom,
        LocalDate previousTo, String timeZone, Instant generatedAt, Metrics metrics, Metrics previousMetrics,
        List<Trend> trend, List<CategoryRevenueResponse> categories, List<ProductSales> topProducts,
        Operations operations, List<OrderSummaryResponse> recentOrders, List<StockAlert> stockAlerts) {
    public record Metrics(long totalOrders, BigDecimal orderedValue, BigDecimal deliveredValue,
            BigDecimal discountAmount, BigDecimal averageOrderValue, long newCustomers,
            long cancelledOrders, long deliveredOrders, long unitsSold, List<OrderStatusBreakdownResponse> statuses) {}
    public record Trend(LocalDate date, BigDecimal orderedValue, BigDecimal deliveredValue) {}
    public record ProductSales(UUID productId, String productName, long quantity, BigDecimal netValue) {}
    public record Operations(long pendingOrders, long confirmedOrders, long shippingOrders,
            long lowStockVariants, long outOfStockVariants) {}
    public record StockAlert(UUID variantId, UUID productId, String productName, String sku,
            int stockQuantity, int lowStockThreshold, String status) {}
}
