package com.example.sport_be.security;

import com.example.sport_be.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Produces the standard {@link ErrorResponse} JSON body with HTTP 403 when
 * an authenticated request without the required role hits a protected
 * route, e.g. a non-admin account calling {@code /api/admin/**}
 * (Requirement 6.7).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final String ERROR_CODE = "FORBIDDEN";

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                ERROR_CODE,
                "Bạn không có quyền truy cập tài nguyên này."
        );
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
