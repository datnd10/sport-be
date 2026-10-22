package com.example.sport_be.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads the {@code Authorization: Bearer <token>} header on every request,
 * validates the JWT, and—if valid—populates the
 * {@link org.springframework.security.core.context.SecurityContext} with an
 * authenticated principal carrying the account's {@code ROLE_*} authority.
 *
 * <p>Invalid, expired, or missing tokens simply leave the security context
 * empty; the downstream {@link org.springframework.security.web.access.ExceptionTranslationFilter}
 * is responsible for turning that into a 401 for routes that require
 * authentication, via {@link RestAuthenticationEntryPoint}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(AUTH_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                authenticate(token, request);
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Rejected invalid JWT: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        Claims claims = jwtService.parseClaims(token);
        UUID accountId = jwtService.extractAccountId(claims);
        String email = jwtService.extractEmail(claims);
        var role = jwtService.extractRole(claims);

        var principal = new JwtPrincipal(accountId, email, role);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
