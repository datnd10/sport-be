package com.example.sport_be.exception;

/**
 * Thrown when an authenticated user does not have the required role or
 * permission to access a resource (e.g. a non-admin account accessing
 * admin-only routes). Maps to HTTP 403 Forbidden in the
 * GlobalExceptionHandler.
 */
public class ForbiddenException extends BaseBusinessException {

    public static final String DEFAULT_ERROR_CODE = "FORBIDDEN";

    public ForbiddenException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message);
    }
}
