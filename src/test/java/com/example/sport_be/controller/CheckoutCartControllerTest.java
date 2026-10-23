package com.example.sport_be.controller;

import com.example.sport_be.dto.order.CreateOrderRequest;
import com.example.sport_be.dto.order.OrderItemRequest;
import com.example.sport_be.entity.*;
import com.example.sport_be.enums.*;
import com.example.sport_be.repository.*;
import com.example.sport_be.security.JwtService;
import com.example.sport_be.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutCartControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository cartItems;
    @Autowired OrderRepository orders;
    @Autowired OrderService orderService;
    @Autowired JwtService jwt;
    @Autowired PlatformTransactionManager transactions;
    Account customer;
    Cart cart;
    Cart otherCart;
    ProductVariant first;
    ProductVariant second;
    String token;

    @BeforeEach void fixture() {
        var suffix = UUID.randomUUID().toString();
        customer = accounts.save(new Account(suffix + "@example.com", "unused", Role.CUSTOMER));
        var other = accounts.save(new Account("other" + suffix + "@example.com", "unused", Role.CUSTOMER));
        token = jwt.generateToken(customer.getId(), customer.getEmail(), customer.getRole());
        var category = categories.save(new Category(suffix, suffix));
        var product = products.save(new Product(category, "Checkout", BigDecimal.valueOf(20000)));
        first = variants.save(new ProductVariant(product, "first" + suffix, "size=40", BigDecimal.valueOf(20000), 20));
        second = variants.save(new ProductVariant(product, "second" + suffix, "size=41", BigDecimal.valueOf(20000), 20));
        cart = carts.save(new Cart(customer));
        otherCart = carts.save(new Cart(other));
        cartItems.save(new CartItem(cart, first, 2));
        cartItems.save(new CartItem(cart, second, 1));
        cartItems.save(new CartItem(otherCart, first, 3));
    }
    String payload(int quantity, boolean both) {
        return "{\"recipientName\":\"Demo\",\"recipientPhone\":\"0900000000\",\"shippingAddress\":\"Ha Noi\",\"paymentMethod\":\"COD\",\"items\":[{\"variantId\":\""
                + first.getId() + "\",\"quantity\":" + quantity + "}"
                + (both ? ",{\"variantId\":\"" + second.getId() + "\",\"quantity\":1}" : "") + "]}";
    }
    @Test void successfulCheckoutClearsPersistedCartAndLeavesOtherCustomersCartUntouched() throws Exception {
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token).contentType("application/json").content(payload(2, true)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
        assertThat(cartItems.findByCart_Id(cart.getId())).isEmpty();
        assertThat(cartItems.findByCart_Id(otherCart.getId())).hasSize(1);
    }
    @Test void failedCheckoutKeepsCartAndDoesNotCreateOrder() throws Exception {
        long before = orders.count();
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token).contentType("application/json").content(payload(100, true)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(cartItems.findByCart_Id(cart.getId())).hasSize(2);
        assertThat(orders.count()).isEqualTo(before);
    }
    @Test void itemsNotIncludedInThePurchaseRemainInCart() throws Exception {
        mvc.perform(post("/api/orders").header("Authorization", "Bearer " + token).contentType("application/json").content(payload(2, false)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].variantId").value(second.getId().toString()));
    }
    @Test void rollbackRestoresCartAsWellAsOrderAndStock() {
        long before = orders.count();
        var tx = new TransactionTemplate(transactions);
        assertThatThrownBy(() -> tx.execute(status -> {
            orderService.createOrder(customer.getId(), new CreateOrderRequest(
                    List.of(new OrderItemRequest(first.getId(), 2)), "Demo", "0900000000", "Ha Noi", PaymentMethod.COD, null));
            throw new IllegalStateException("Simulated transaction failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(cartItems.findByCart_Id(cart.getId())).hasSize(2);
        assertThat(orders.count()).isEqualTo(before);
        assertThat(variants.findById(first.getId()).orElseThrow().getStockQuantity()).isEqualTo(20);
    }
}
