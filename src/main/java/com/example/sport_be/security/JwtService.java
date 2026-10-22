package com.example.sport_be.security;

import com.example.sport_be.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Issues and validates JWT access tokens used for stateless authentication.
 *
 * <p>The token subject is the {@link com.example.sport_be.entity.Account} id
 * and carries the account's {@code email} and {@code role} as claims, so the
 * {@link JwtAuthenticationFilter} can build an authenticated
 * {@link org.springframework.security.core.Authentication} without a
 * database lookup on every request.
 */
@Component
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT for the given account identity.
     */
    public String generateToken(UUID accountId, String email, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(accountId.toString())
                .claims(Map.of(CLAIM_EMAIL, email, CLAIM_ROLE, role.name()))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates the token, returning its claims.
     *
     * @throws JwtException if the token is malformed, expired, or has an invalid signature
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractAccountId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    public Role extractRole(Claims claims) {
        return Role.valueOf(claims.get(CLAIM_ROLE, String.class));
    }
}
