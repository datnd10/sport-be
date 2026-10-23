package com.example.sport_be.repository;

import com.example.sport_be.entity.Account;
import com.example.sport_be.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.id = :id")
    Optional<Account> findForPasswordChange(@org.springframework.data.repository.query.Param("id") UUID id);

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Counts Tài_Khoản with the given role created within {@code [from,
     * to)}, for the Dashboard summary's "new customers" figure
     * (Requirement 10).
     */
    long countByRoleAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(Role role, Instant from, Instant to);
}
