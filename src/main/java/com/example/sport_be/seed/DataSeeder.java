package com.example.sport_be.seed;

import com.example.sport_be.entity.Account;
import com.example.sport_be.entity.Category;
import com.example.sport_be.entity.DiscountCode;
import com.example.sport_be.entity.Product;
import com.example.sport_be.entity.ProductImage;
import com.example.sport_be.entity.ProductVariant;
import com.example.sport_be.enums.DiscountType;
import com.example.sport_be.enums.Role;
import com.example.sport_be.repository.AccountRepository;
import com.example.sport_be.repository.CategoryRepository;
import com.example.sport_be.repository.DiscountCodeRepository;
import com.example.sport_be.repository.ProductImageRepository;
import com.example.sport_be.repository.ProductRepository;
import com.example.sport_be.repository.ProductVariantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Seeds the database with a minimal, realistic dataset the first time the
 * application starts against an empty database, so the storefront and
 * admin UI have something to show out of the box during local development.
 *
 * <p>Seeding is idempotent and guarded by {@link AccountRepository#count()}:
 * if any {@link Account} already exists (i.e. the database has been seeded
 * or used before), the runner does nothing. It also only runs outside the
 * {@code test} profile so the (H2, create-drop) test database is never
 * touched by it.
 */
@Configuration
@Profile("!test")
@Slf4j
public class DataSeeder {

    private static final String ADMIN_EMAIL = "admin@lananhsport.vn";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String CUSTOMER_EMAIL = "customer@lananhsport.vn";
    private static final String CUSTOMER_PASSWORD = "Customer@123";

    @Bean
    @Transactional
    public CommandLineRunner seedInitialData(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (accountRepository.count() > 0) {
                log.info("DataSeeder: accounts already exist, skipping initial data seed.");
                return;
            }

            log.info("DataSeeder: empty database detected, seeding initial data...");

            seedAccounts(accountRepository, passwordEncoder);

            log.info("DataSeeder: done. Admin login: {} / {} | Customer login: {} / {}",
                    ADMIN_EMAIL, ADMIN_PASSWORD, CUSTOMER_EMAIL, CUSTOMER_PASSWORD);
        };
    }

    private void seedAccounts(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        Account admin = new Account(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN);
        admin.setFullName("Quan Tri Vien");
        admin.setPhone("0900000001");
        admin.setAddress("123 Nguyen Trai, Quan 1, TP.HCM");
        accountRepository.save(admin);

        Account customer = new Account(CUSTOMER_EMAIL, passwordEncoder.encode(CUSTOMER_PASSWORD), Role.CUSTOMER);
        customer.setFullName("Nguyen Van Khach");
        customer.setPhone("0900000002");
        customer.setAddress("456 Le Loi, Quan 3, TP.HCM");
        accountRepository.save(customer);
    }
}
