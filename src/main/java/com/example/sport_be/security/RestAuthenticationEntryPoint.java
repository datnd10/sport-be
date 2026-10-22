package com.example.sport_be.security;

import com.example.sport_be.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Produces the standard {@link ErrorResponse} JSON body with HTTP 401 when
 * an unauthenticated request hits a protected route (Requirement 6.5).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String ERROR_CODE = "UNAUTHORIZED";

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ERROR_CODE,
                "Bạn cần đăng nhập để truy cập tài nguyên này."
        );
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
