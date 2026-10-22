package com.example.sport_be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serves uploaded Hình_Ảnh_Sản_Phẩm files straight off local disk via
 * Spring's static resource handler, as described in design.md ("Lưu trữ
 * hình ảnh Sản_Phẩm"). Files stored under {@code uploads/products/...}
 * become reachable at {@code /uploads/products/...}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebConfig(@Value("${app.upload.root-dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
