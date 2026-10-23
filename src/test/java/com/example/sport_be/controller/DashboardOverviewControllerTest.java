package com.example.sport_be.controller;

import com.example.sport_be.entity.*;
import com.example.sport_be.enums.*;
import com.example.sport_be.repository.*;
import com.example.sport_be.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.jpa.open-in-view=false", "spring.datasource.url=jdbc:h2:mem:dashboard_overview_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardOverviewControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired JwtService jwt;

    String token(Role role) {
        var account = accounts.save(new Account(UUID.randomUUID() + "@example.com", "unused", role));
        return jwt.generateToken(account.getId(), account.getEmail(), role);
    }
    ProductVariant variant(String categoryName, int stock, boolean active) {
        var category = categories.save(new Category(categoryName, UUID.randomUUID().toString()));
        var product = products.save(new Product(category, categoryName + " product", BigDecimal.TEN));
        var variant = new ProductVariant(product, UUID.randomUUID().toString(), "size=40", BigDecimal.TEN, stock);
        variant.setActive(active);
        return variants.save(variant);
    }
    Order order(OrderStatus status, String total, String discount, String createdAt) {
        var amount = new BigDecimal(total);
        var reduction = new BigDecimal(discount);
        var order = new Order(null, PaymentMethod.COD, amount.add(reduction), amount, "Customer", "0900000000", "Ha Noi");
        order.setStatus(status); order.setDiscountAmount(reduction);
        order = orders.save(order);
        jdbc.update("UPDATE orders SET created_at = ? WHERE id = ?", java.sql.Timestamp.from(java.time.Instant.parse(createdAt)), order.getId());
        return order;
    }
    @Test void overviewUsesVietnamDatesAndReconcilesNetValuesAcrossMetricsCategoriesAndProducts() throws Exception {
        var a = variant("Category A", 0, true);
        var b = variant("Category B", 2, true);
        var hidden = variant("Hidden", 0, false);
        var inactiveProduct = variant("Inactive product", 0, true);
        var product = products.findById(inactiveProduct.getProduct().getId()).orElseThrow();
        product.setActive(false); products.save(product);
        var delivered = order(OrderStatus.DELIVERED, "7", "3", "2026-01-01T17:30:00Z"); // 00:30 Jan 2 in Vietnam
        items.save(new OrderItem(delivered, a, "A", new BigDecimal("3"), 1));
        items.save(new OrderItem(delivered, b, "B", new BigDecimal("7"), 1));
        var pending = order(OrderStatus.PENDING, "3", "0", "2026-01-02T02:00:00Z");
        items.save(new OrderItem(pending, a, "A", new BigDecimal("3"), 1));
        var cancelled = order(OrderStatus.CANCELLED, "100", "0", "2026-01-02T03:00:00Z");
        items.save(new OrderItem(cancelled, a, "A", new BigDecimal("100"), 1));
        var previous = order(OrderStatus.CONFIRMED, "2", "0", "2026-01-01T16:59:59Z"); // Jan 1 in Vietnam
        items.save(new OrderItem(previous, a, "A", new BigDecimal("2"), 1));
        var response = mvc.perform(get("/api/admin/dashboard/overview").param("range", "custom")
                        .param("from", "2026-01-02").param("to", "2026-01-02").header("Authorization", "Bearer " + token(Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.timeZone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.metrics.totalOrders").value(3))
                .andExpect(jsonPath("$.metrics.orderedValue").value(10))
                .andExpect(jsonPath("$.metrics.deliveredValue").value(7))
                .andExpect(jsonPath("$.metrics.discountAmount").value(3))
                .andExpect(jsonPath("$.metrics.averageOrderValue").value(5))
                .andExpect(jsonPath("$.metrics.unitsSold").value(3))
                .andExpect(jsonPath("$.metrics.cancelledOrders").value(1))
                .andExpect(jsonPath("$.previousMetrics.orderedValue").value(2))
                .andExpect(jsonPath("$.operations.pendingOrders").value(1))
                .andExpect(jsonPath("$.operations.confirmedOrders").value(1))
                .andExpect(jsonPath("$.operations.lowStockVariants").value(1))
                .andExpect(jsonPath("$.operations.outOfStockVariants").value(1))
                .andExpect(jsonPath("$.stockAlerts.length()").value(2))
                .andExpect(jsonPath("$.trend.length()").value(1)).andReturn();
        var root = json.readTree(response.getResponse().getContentAsString());
        BigDecimal categoryTotal = BigDecimal.ZERO, productTotal = BigDecimal.ZERO;
        for (var category : root.get("categories")) categoryTotal = categoryTotal.add(category.get("revenue").decimalValue());
        for (var sales : root.get("topProducts")) productTotal = productTotal.add(sales.get("netValue").decimalValue());
        assertThat(categoryTotal).isEqualByComparingTo("10");
        assertThat(productTotal).isEqualByComparingTo("10");
        assertThat(root.get("trend").get(0).get("orderedValue").decimalValue()).isEqualByComparingTo("10");
        mvc.perform(get("/api/admin/inventory").param("stockStatus", "OUT_OF_STOCK").header("Authorization", "Bearer " + token(Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].variantId").value(a.getId().toString()));
    }
    @Test void enforcesAdminAccessAndValidatesDateRanges() throws Exception {
        mvc.perform(get("/api/admin/dashboard/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard/overview").header("Authorization", "Bearer " + token(Role.CUSTOMER)))
                .andExpect(status().isForbidden());
        String admin = token(Role.ADMIN);
        for (var dates : new String[][]{{"2026-01-03", "2026-01-02"}, {"2020-01-01", "2026-01-01"}, {"2999-01-01", "2999-01-02"}}) {
            mvc.perform(get("/api/admin/dashboard/overview").param("range", "custom").param("from", dates[0]).param("to", dates[1])
                    .header("Authorization", "Bearer " + admin)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/admin/dashboard/overview").param("range", "unknown").header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/dashboard/overview").param("range", "custom").header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }
    @Test void fillsEveryDayForAnEmptyPeriodWithoutFakeChanges() throws Exception {
        mvc.perform(get("/api/admin/dashboard/overview").param("range", "custom").param("from", "2020-03-01").param("to", "2020-03-03")
                        .header("Authorization", "Bearer " + token(Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.metrics.totalOrders").value(0))
                .andExpect(jsonPath("$.metrics.orderedValue").value(0))
                .andExpect(jsonPath("$.metrics.averageOrderValue").value(0))
                .andExpect(jsonPath("$.trend.length()").value(3))
                .andExpect(jsonPath("$.trend[1].orderedValue").value(0))
                .andExpect(jsonPath("$.categories.length()").value(0));
    }
}
