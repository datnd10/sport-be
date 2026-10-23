package com.example.sport_be.controller;

import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.enums.DiscountType;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.DiscountCodeRepository;
import com.example.sport_be.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AvailableDiscountsControllerTest {
    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired DiscountCodeRepository codes;
    @Autowired JwtService jwt;

    @Test void listsOnlyUsableOffersForCustomersAndRevalidatesOnApply() throws Exception {
        var suffix = UUID.randomUUID().toString();
        var customer = accounts.save(new Account(suffix + "@example.com", "unused", Role.CUSTOMER));
        var token = jwt.generateToken(customer.getId(), customer.getEmail(), customer.getRole());
        var now = Instant.now();
        var valid = codes.save(new DiscountCode("VALID" + suffix, DiscountType.PERCENTAGE, BigDecimal.TEN, now.minusSeconds(60), now.plusSeconds(3600), 10));
        var fixed = codes.save(new DiscountCode("FIXED" + suffix, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(50000), now.minusSeconds(60), now.plusSeconds(3600), 10));
        var expired = codes.save(new DiscountCode("EXPIRED" + suffix, DiscountType.PERCENTAGE, BigDecimal.TEN, now.minusSeconds(3600), now.minusSeconds(60), 10));
        var future = codes.save(new DiscountCode("FUTURE" + suffix, DiscountType.PERCENTAGE, BigDecimal.TEN, now.plusSeconds(60), now.plusSeconds(3600), 10));
        var inactive = new DiscountCode("INACTIVE" + suffix, DiscountType.PERCENTAGE, BigDecimal.TEN, now.minusSeconds(60), now.plusSeconds(3600), 10);
        inactive.setActive(false); codes.save(inactive);
        var exhausted = new DiscountCode("EXHAUSTED" + suffix, DiscountType.PERCENTAGE, BigDecimal.TEN, now.minusSeconds(60), now.plusSeconds(3600), 10);
        exhausted.setUsageCount(10); codes.save(exhausted);
        mvc.perform(get("/api/discount-codes/available").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[*].code", hasItems(valid.getCode(), fixed.getCode())))
                .andExpect(jsonPath("$[*].code", not(hasItems(expired.getCode()))))
                .andExpect(jsonPath("$[*].code", not(hasItems(future.getCode()))))
                .andExpect(jsonPath("$[*].code", not(hasItems(inactive.getCode()))))
                .andExpect(jsonPath("$[*].code", not(hasItems(exhausted.getCode()))))
                .andExpect(jsonPath("$[0].validTo").exists())
                .andExpect(jsonPath("$[0].maxUsage").doesNotExist());
        valid.setActive(false); codes.save(valid);
        mvc.perform(get("/api/discount-codes/validate").param("code", valid.getCode()).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("DISCOUNT_CODE_INACTIVE"));
    }
    @Test void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/discount-codes/available")).andExpect(status().isUnauthorized());
    }
}
