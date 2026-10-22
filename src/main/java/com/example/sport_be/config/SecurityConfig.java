package com.example.sport_be.config;

import com.example.sport_be.security.AccountUserDetailsService;
import com.example.sport_be.security.JwtAuthenticationFilter;
import com.example.sport_be.security.JwtService;
import com.example.sport_be.security.RestAccessDeniedHandler;
import com.example.sport_be.security.RestAuthenticationEntryPoint;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Central Spring Security configuration: stateless JWT authentication,
 * BCrypt password hashing, and role-based access control.
 *
 * <p>Route {@code /api/admin/**} requires {@code ROLE_ADMIN} (Requirement
 * 6.7); any other route that isn't explicitly public requires a valid,
 * authenticated session (Requirement 6.5). Unauthenticated and
 * unauthorized requests are turned into the standard JSON error shape by
 * {@link RestAuthenticationEntryPoint} (401) and {@link
 * RestAccessDeniedHandler} (403) respectively. The static
 * {@code /uploads/**} path (uploaded Hình_Ảnh_Sản_Phẩm files, see {@link
 * WebConfig}) is also public for GET so product images render in plain
 * {@code <img>} tags without a JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Public (unauthenticated) GET endpoints, per the endpoint tables in
     * design.md. Write operations under these same paths (e.g. creating a
     * review) still require authentication and are not included here.
     *
     * <p>{@code /uploads/**} is included here too: it is not an
     * {@code /api/**} route but the static resource handler registered in
     * {@link WebConfig} that serves uploaded Hình_Ảnh_Sản_Phẩm files
     * straight off disk. Those images must be viewable on both the public
     * storefront and the Admin product pages, and a plain {@code <img
     * src="...">} load in the browser never sends the {@code
     * Authorization} header used for JWT auth. Without this entry, every
     * image request falls through to {@code anyRequest().authenticated()}
     * and gets rejected with 401, which is why product image previews
     * failed to load in the Admin UI. Uploading/replacing/deleting images
     * is a separate concern handled via the authenticated {@code
     * /api/admin/products/{id}/images} routes, not through this static
     * path, so making {@code /uploads/**} public only exposes read access
     * to already-uploaded files.
     */
    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/categories",
            "/api/categories/**",
            "/api/products",
            "/api/products/**",
            "/api/discount-codes/validate",
            "/uploads/**",
            "/api/health"
    };

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**"
    };

    /**
     * Public (unauthenticated) POST endpoints. {@code POST /api/orders}
     * must allow Khách_Hàng vãng lai (guests) to place an order, per the
     * Customer/Guest role listed for this endpoint in design.md;
     * {@code OrderController} still associates the order with thethe
     * caller's Account when a valid JWT is present.
     */
    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/orders"
    };

    /**
     * Allowed CORS origins for the frontend (sport-fe), configurable via
     * the {@code app.cors.allowed-origins} property so staging/production
     * deployments can point at their real frontend origin(s) without code
     * changes. Defaults cover the local Vite dev server.
     */
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            AccountUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    /**
     * CORS configuration applied to every {@code /api/**} route so the
     * frontend (served from a different origin/port in dev and prod) can
     * call the API directly from the browser, including with the
     * {@code Authorization} header used for JWT auth.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}