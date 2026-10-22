package com.example.sport_be.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests asserting that {@code /uploads/**} (the static
 * resource handler in {@link WebConfig} that serves uploaded
 * Hình_Ảnh_Sản_Phẩm files) is reachable without authentication, so
 * product image previews render correctly on both the storefront and the
 * Admin UI. A plain {@code <img src="...">} load never sends the {@code
 * Authorization} header, so this route must not fall through to {@code
 * anyRequest().authenticated()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getUploads_withoutAuthToken_isNotRejectedAsUnauthorized() throws Exception {
        // The file itself doesn't need to exist on disk: a 404 (resource
        // not found) proves the request passed the authentication filter
        // chain, which is what we're verifying here. A 401 would mean
        // /uploads/** is still being treated as a protected route.
        mockMvc.perform(get("/uploads/products/does-not-exist.jpg"))
                .andExpect(result -> {
                    int statusCode = result.getResponse().getStatus();
                    org.assertj.core.api.Assertions.assertThat(statusCode)
                            .as("GET /uploads/** without a token must not be rejected as unauthorized")
                            .isNotEqualTo(401);
                });
    }

    @Test
    void getAdminRoute_withoutAuthToken_isRejectedAsUnauthorized() throws Exception {
        // Sanity check in the same test class: unrelated admin routes must
        // still require authentication, so the /uploads/** fix doesn't
        // accidentally widen access elsewhere.
        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isUnauthorized());
    }
}
