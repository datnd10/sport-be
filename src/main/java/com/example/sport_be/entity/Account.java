package com.example.sport_be.entity;

import com.example.sport_be.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a user account in the system, either a customer or an admin.
 *
 * <p>Holds authentication data (email, password hash) and profile data
 * (full name, phone, address) that can be updated by the account owner.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "full_name")
    private String fullName;

    private String phone;

    private String address;

    /**
     * Whether this Tài_Khoản is allowed to log in. Defaults to {@code
     * true} for every newly created account. An Admin (Quản_Trị_Viên) may
     * khóa (lock) a Tài_Khoản by setting this to {@code false} via the
     * Admin Tài_Khoản management page; {@code AuthService.login} rejects
     * the login attempt with a {@code ForbiddenException} while this
     * flag is {@code false}, even when the email/password is correct.
     */
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    public Account(String email, String passwordHash, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}
