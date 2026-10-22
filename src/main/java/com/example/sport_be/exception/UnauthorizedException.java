package com.example.sport_be.exception;

/**
 * Thrown when a request is made without valid authentication (e.g. a
 * protected route is accessed without a valid JWT). Maps to HTTP 401
 * Unauthorized in the GlobalExceptionHandler.
 */
public class UnauthorizedException extends BaseBusinessException {

    public static final String DEFAULT_ERROR_CODE = "UNAUTHORIZED";

    public UnauthorizedException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message);
    }
}
