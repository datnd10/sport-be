package com.example.sport_be.controller;

import com.example.sport_be.entity.*;
import com.example.sport_be.enums.*;
import com.example.sport_be.repository.*;
import com.example.sport_be.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderViewsControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired ProductImageRepository images;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired JwtService jwt;
    String token;
    Order order;
    Product product;
    @BeforeEach void fixture() {
        var suffix = UUID.randomUUID().toString();
        var account = accounts.save(new Account(suffix + "@example.com", "unused", Role.CUSTOMER));
        token = jwt.generateToken(account.getId(), account.getEmail(), account.getRole());
        var category = categories.save(new Category(suffix, suffix));
        product = products.save(new Product(category, "Current name", BigDecimal.valueOf(90000)));
        images.save(new ProductImage(product, "uploads/later.jpg", 5));
        images.save(new ProductImage(product, "uploads/first.jpg", 0));
        var variant = variants.save(new ProductVariant(product, suffix, "size=40", BigDecimal.valueOf(90000), 10));
        order = orders.save(new Order(account, PaymentMethod.BANK_TRANSFER, BigDecimal.valueOf(40000),
                BigDecimal.valueOf(40000), "Recipient", "0900000000", "Ha Noi"));
        items.save(new OrderItem(order, variant, "Purchased name", BigDecimal.valueOf(20000), 2));
    }
    @Test void historyIncludesThumbnailAndPurchasedPriceWithOsivDisabled() throws Exception {
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].items[0].productName").value("Purchased name"))
                .andExpect(jsonPath("$[0].items[0].unitPrice").value(20000))
                .andExpect(jsonPath("$[0].items[0].lineTotal").value(40000))
                .andExpect(jsonPath("$[0].items[0].imagePath").value("uploads/first.jpg"));
    }
    @Test void detailReturnsCompleteItemAndBankInformation() throws Exception {
        mvc.perform(get("/api/orders/" + order.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").exists())
                .andExpect(jsonPath("$.items[0].productId").value(product.getId().toString()))
                .andExpect(jsonPath("$.items[0].attributes").value("size=40"))
                .andExpect(jsonPath("$.items[0].productNameSnapshot").value("Purchased name"))
                .andExpect(jsonPath("$.bankTransferInfo.accountNumber").isNotEmpty());
    }
    @Test void customerCannotReadSomeoneElsesOrderOrListIt() throws Exception {
        var other = accounts.save(new Account(UUID.randomUUID() + "@example.com", "unused", Role.CUSTOMER));
        var otherToken = jwt.generateToken(other.getId(), other.getEmail(), other.getRole());
        mvc.perform(get("/api/orders/" + order.getId()).header("Authorization", "Bearer " + otherToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/orders").header("Authorization", "Bearer " + otherToken)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }
}
