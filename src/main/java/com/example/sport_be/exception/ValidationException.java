package com.example.sport_be.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when request data fails business validation (e.g. missing
 * required fields). Maps to HTTP 400 Bad Request in the
 * GlobalExceptionHandler.
 * <p>
 * Carries a list of {@link FieldError} so the standard error DTO's
 * {@code fieldErrors} field can describe exactly which fields are invalid.
 */
public class ValidationException extends BaseBusinessException {

    public static final String DEFAULT_ERROR_CODE = "VALIDATION_ERROR";

    private final List<FieldError> fieldErrors;

    public ValidationException(String message) {
        this(DEFAULT_ERROR_CODE, message, Collections.emptyList());
    }

    public ValidationException(String message, List<FieldError> fieldErrors) {
        this(DEFAULT_ERROR_CODE, message, fieldErrors);
    }

    public ValidationException(String errorCode, String message, List<FieldError> fieldErrors) {
        super(errorCode, message);
        this.fieldErrors = fieldErrors == null ? Collections.emptyList() : List.copyOf(fieldErrors);
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
