package com.example.sport_be.exception;

/**
 * Base class for all business/domain exceptions in the application.
 * Carries an {@code errorCode} used to populate the standard error
 * response DTO's {@code errorCode} field.
 */
public abstract class BaseBusinessException extends RuntimeException {

    private final String errorCode;

    protected BaseBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected BaseBusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
