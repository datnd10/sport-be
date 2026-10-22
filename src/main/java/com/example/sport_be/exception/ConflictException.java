package com.example.sport_be.exception;

/**
 * Thrown when a request conflicts with existing data due to a uniqueness
 * constraint (e.g. duplicate SKU, duplicate email, duplicate discount
 * code). Maps to HTTP 409 Conflict in the GlobalExceptionHandler.
 */
public class ConflictException extends BaseBusinessException {

    public static final String DEFAULT_ERROR_CODE = "CONFLICT";

    public ConflictException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }
}
