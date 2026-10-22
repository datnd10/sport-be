package com.example.sport_be.utils;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCodeGeneratorTest {

    private static final int SANITY_CHECK_ITERATIONS = 1000;

    @Test
    void generate_producesCodeMatchingExpectedFormat() {
        String code = OrderCodeGenerator.generate();
        assertThat(code).matches("^ORD-\\d{8}-[A-Z0-9]{6}$");
    }

    @Test
    void generate_producesDifferentCodesAcrossManyCalls() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < SANITY_CHECK_ITERATIONS; i++) {
            codes.add(OrderCodeGenerator.generate());
        }
        assertThat(codes).hasSize(SANITY_CHECK_ITERATIONS);
    }
}
