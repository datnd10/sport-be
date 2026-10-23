package com.example.sport_be.controller;

import com.example.sport_be.entity.*;
import com.example.sport_be.entity.Order;
import com.example.sport_be.enums.*;
import com.example.sport_be.repository.*;
import com.example.sport_be.security.JwtService;
import com.example.sport_be.service.ReviewSubmissionService;
import com.example.sport_be.dto.review.CreateReviewRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:review_images_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewImagesControllerTest {
    static final Path UPLOAD;
    static { try { UPLOAD = Files.createTempDirectory("sport-review-images-"); } catch (Exception ex) { throw new RuntimeException(ex); } }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("app.upload.root-dir", () -> UPLOAD.toString()); }
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired ProductReviewRepository reviews;
    @Autowired ProductReviewImageRepository images;
    @Autowired JwtService jwt;
    @Autowired ReviewSubmissionService submissions;
    @Autowired PlatformTransactionManager transactions;
    Account customer;
    Product product;
    Order order;
    OrderItem item;
    String token;

    @BeforeEach void fixture() {
        String suffix = UUID.randomUUID().toString();
        customer = accounts.save(new Account(suffix + "@example.com", "unused", Role.CUSTOMER));
        token = jwt.generateToken(customer.getId(), customer.getEmail(), customer.getRole());
        var category = categories.save(new Category("Review images " + suffix, suffix));
        product = products.save(new Product(category, "Review images " + suffix, BigDecimal.TEN));
        var variant = variants.save(new ProductVariant(product, suffix, "size=40", BigDecimal.TEN, 10));
        order = new Order(customer, PaymentMethod.COD, BigDecimal.TEN, BigDecimal.TEN, "Demo", "0000000000", "Demo");
        order.setStatus(OrderStatus.DELIVERED);
        order = orders.save(order);
        item = items.save(new OrderItem(order, variant, product.getName(), BigDecimal.TEN, 1));
    }
    MockMultipartFile request() {
        return new MockMultipartFile("review", "review.json", "application/json",
                ("{\"orderItemId\":\"" + item.getId() + "\",\"rating\":5,\"comment\":\"Photo review\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    MockMultipartFile png() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        return new MockMultipartFile("images", "photo.png", "image/png", out.toByteArray());
    }
    String path() { return "/api/products/" + product.getId() + "/reviews"; }

    @Test void uploadsAndReturnsImagesAndExcludesHiddenReview() throws Exception {
        long before = fileCount();
        var result = mvc.perform(multipart(path()).file(request()).file(png()).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].displayOrder").value(0)).andReturn();
        assertThat(fileCount()).isEqualTo(before + 1);
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
        String filePath = json.get("images").get(0).get("filePath").asText();
        assertThat(Files.exists(UPLOAD.resolve(filePath.substring("uploads/".length())))).isTrue();
        mvc.perform(get(path())).andExpect(status().isOk()).andExpect(jsonPath("$.reviews[0].images[0].filePath").value(filePath));
        var review = reviews.findByOrderItem_Id(item.getId()).orElseThrow();
        review.setHidden(true); reviews.save(review);
        mvc.perform(get(path())).andExpect(status().isOk()).andExpect(jsonPath("$.reviews.length()").value(0));
        assertThat(result.getResponse().getContentAsString()).doesNotContain(UPLOAD.toString());
    }
    @Test void rejectsFakeImagesWithoutPersistingReviewOrFiles() throws Exception {
        long before = fileCount();
        mvc.perform(multipart(path()).file(request()).file(new MockMultipartFile("images", "fake.png", "image/png", "not an image".getBytes()))
                .header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        assertThat(reviews.findByOrderItem_Id(item.getId())).isEmpty();
        assertThat(fileCount()).isEqualTo(before);
    }
    @Test void rejectsMoreThanFivePhotos() throws Exception {
        var builder = multipart(path()).file(request());
        builder.header("Authorization", "Bearer " + token);
        for (int i = 0; i < 6; i++) builder.file(png());
        mvc.perform(builder).andExpect(status().isBadRequest());
        assertThat(reviews.findByOrderItem_Id(item.getId())).isEmpty();
    }
    @Test void requiresAuthenticationAndDeliveredOrderOwner() throws Exception {
        mvc.perform(multipart(path()).file(request()).file(png())).andExpect(status().isUnauthorized());
        var other = accounts.save(new Account(UUID.randomUUID() + "@example.com", "unused", Role.CUSTOMER));
        String otherToken = jwt.generateToken(other.getId(), other.getEmail(), other.getRole());
        mvc.perform(multipart(path()).file(request()).file(png()).header("Authorization", "Bearer " + otherToken)).andExpect(status().isUnprocessableEntity());
        for (var status : List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.CANCELLED)) {
            order.setStatus(status); orders.save(order);
            mvc.perform(multipart(path()).file(request()).file(png()).header("Authorization", "Bearer " + token)).andExpect(status().isUnprocessableEntity());
        }
        assertThat(reviews.findByOrderItem_Id(item.getId())).isEmpty();
    }
    @Test void preventsDuplicatesAndEligibilityOnlyIncludesUnreviewedPurchases() throws Exception {
        mvc.perform(get("/api/account/review-eligibility").param("productId", product.getId().toString()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(multipart(path()).file(request()).file(png()).header("Authorization", "Bearer " + token)).andExpect(status().isCreated());
        long before = fileCount();
        mvc.perform(multipart(path()).file(request()).file(png()).header("Authorization", "Bearer " + token)).andExpect(status().isUnprocessableEntity());
        assertThat(fileCount()).isEqualTo(before);
        mvc.perform(get("/api/account/review-eligibility").param("productId", product.getId().toString()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void rollbackRemovesWrittenFilesAndDatabaseRows() throws Exception {
        long before = fileCount();
        var photo = png();
        var tx = new TransactionTemplate(transactions);
        assertThatThrownBy(() -> tx.execute(status -> {
            submissions.submit(customer.getId(), product.getId(), new CreateReviewRequest(item.getId(), 5, "Rollback"), List.of(photo));
            throw new IllegalStateException("Simulated commit failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(reviews.findByOrderItem_Id(item.getId())).isEmpty();
        assertThat(fileCount()).isEqualTo(before);
    }
    @Test void supportsJsonTextOnlyReviews() throws Exception {
        mvc.perform(post(path()).header("Authorization", "Bearer " + token).contentType("application/json")
                .content("{\"rating\":4,\"comment\":\"Text only\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.images.length()").value(0));
    }
    long fileCount() throws Exception { try (var stream = Files.walk(UPLOAD)) { return stream.filter(Files::isRegularFile).count(); } }
    @AfterAll static void cleanup() throws Exception {
        try (var stream = Files.walk(UPLOAD)) { for (var path : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
    }
}
