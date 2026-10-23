package com.example.sport_be.controller;

import com.example.sport_be.entity.*;
import com.example.sport_be.enums.Role;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Intentionally no test transaction: DTOs must work with OSIV disabled.
@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired ProductImageRepository productImages;
    @Autowired CartRepository carts;
    @Autowired CartItemRepository items;
    @Autowired JwtService jwt;
    Account customer;
    ProductVariant variant;
    String token;
    @BeforeEach void fixture() {
        String suffix = UUID.randomUUID().toString();
        customer = accounts.save(new Account(suffix + "@example.com", "unused", Role.CUSTOMER));
        token = jwt.generateToken(customer.getId(), customer.getEmail(), customer.getRole());
        var category = categories.save(new Category("Cart " + suffix, suffix));
        var product = products.save(new Product(category, "Cart product", BigDecimal.valueOf(20000)));
        productImages.save(new ProductImage(product, "uploads/later.jpg", 5));
        productImages.save(new ProductImage(product, "uploads/thumbnail.jpg", 0));
        variant = variants.save(new ProductVariant(product, suffix, "size=40", BigDecimal.valueOf(20000), 10));
    }
    @Test void readsPersistedCartWithLazyVariantAndProduct() throws Exception {
        var cart = carts.save(new Cart(customer));
        items.save(new CartItem(cart, variant, 2));
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].productName").value("Cart product"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(20000))
                .andExpect(jsonPath("$.items[0].imagePath").value("uploads/thumbnail.jpg"))
                .andExpect(jsonPath("$.items[0].productId").exists())
                .andExpect(jsonPath("$.items[0].stockQuantity").value(10))
                .andExpect(jsonPath("$.totalAmount").value(40000));
    }
    @Test void allMutationResponsesResolveLazyAssociations() throws Exception {
        mvc.perform(post("/api/cart/items").header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("{\"variantId\":\"" + variant.getId() + "\",\"quantity\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].productName").value("Cart product"));
        var cart = carts.findByAccount_Id(customer.getId()).orElseThrow();
        var item = items.findByCart_Id(cart.getId()).getFirst();
        mvc.perform(put("/api/cart/items/" + item.getId()).header("Authorization", "Bearer " + token).contentType("application/json").content("{\"quantity\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(60000));
        mvc.perform(post("/api/cart/merge").header("Authorization", "Bearer " + token).contentType("application/json")
                        .content("[{\"variantId\":\"" + variant.getId() + "\",\"quantity\":1}]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].quantity").value(4));
        mvc.perform(delete("/api/cart/items/" + item.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0)).andExpect(jsonPath("$.totalAmount").value(0));
    }
    @Test void emptyCartReturnsAValidResponse() throws Exception {
        mvc.perform(get("/api/cart").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0)).andExpect(jsonPath("$.totalAmount").value(0));
    }
}
