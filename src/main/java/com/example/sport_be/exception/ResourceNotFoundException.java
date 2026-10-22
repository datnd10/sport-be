package com.example.sport_be.exception;

/**
 * Thrown when a requested resource (e.g. product, order, variant) does
 * not exist. Maps to HTTP 404 Not Found in the GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends BaseBusinessException {

    public static final String DEFAULT_ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
