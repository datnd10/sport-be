package com.example.sport_be.controller;

import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.Order;
import com.example.sport_be.enums.OrderStatus;
import com.example.sport_be.enums.PaymentMethod;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.OrderRepository;
import com.example.sport_be.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@code GET /api/admin/orders}, covering
 * pagination, keyword search (order code and customer name), and
 * status filtering for the Admin Đơn_Hàng management list
 * (Requirement 5.2).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminOrderListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrderRepository orderRepository;

    private String adminToken;
    private String suffix;
    private Order pendingOrder;
    private Order confirmedOrder;
    private Order guestOrder;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString();

        Account admin = new Account("admin-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.ADMIN);
        admin = accountRepository.save(admin);
        adminToken = jwtService.generateToken(admin.getId(), admin.getEmail(), admin.getRole());

        Account customer = new Account("customer-" + suffix + "@example.com", passwordEncoder.encode("password123"), Role.CUSTOMER);
        customer.setFullName("Nguyen Van A " + suffix);
        customer = accountRepository.save(customer);

        pendingOrder = new Order(customer, PaymentMethod.COD, BigDecimal.TEN, BigDecimal.TEN,
                "Nguyen Van A", "0900000000", "123 Đường ABC");
        pendingOrder = orderRepository.save(pendingOrder);

        confirmedOrder = new Order(customer, PaymentMethod.COD, BigDecimal.TEN, BigDecimal.TEN,
                "Nguyen Van A", "0900000000", "123 Đường ABC");
        confirmedOrder.setStatus(OrderStatus.CONFIRMED);
        confirmedOrder = orderRepository.save(confirmedOrder);

        guestOrder = new Order(null, PaymentMethod.COD, BigDecimal.TEN, BigDecimal.TEN,
                "Khach Le " + suffix, "0911111111", "456 Đường XYZ");
        guestOrder = orderRepository.save(guestOrder);
    }

    @Test
    void listAllOrders_withSmallPageSize_paginatesMatchingOrders() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("keyword", "Nguyen Van A " + suffix)
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content.length()", is(1)));
    }

    @Test
    void listAllOrders_withKeywordMatchingOrderCode_returnsOnlyThatOrder() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("keyword", pendingOrder.getOrderCode())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].orderCode", is(pendingOrder.getOrderCode())));
    }

    @Test
    void listAllOrders_withKeywordMatchingCustomerName_returnsOnlyMatchingAccountOrders() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("keyword", "Nguyen Van A " + suffix)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void listAllOrders_withStatusFilter_returnsOnlyMatchingStatusOrders() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("keyword", "Nguyen Van A " + suffix)
                        .param("status", "CONFIRMED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].status", is("CONFIRMED")));
    }

    @Test
    void listAllOrders_withInvalidStatus_fallsBackToNoStatusFilterInsteadOfFailing() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("keyword", "Nguyen Van A " + suffix)
                        .param("status", "NOT_A_REAL_STATUS")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void listAllOrders_guestOrderIsOnlyMatchedByOrderCodeNotCustomerKeyword() throws Exception {
        mockMvc.perform(get("/api/admin/orders")
                        .param("keyword", guestOrder.getOrderCode())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].accountEmail").doesNotExist());
    }

    @Test
    void listAllOrders_withoutToken_isRejectedAsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }
}
