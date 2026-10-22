package com.example.sport_be.dto;

import com.example.sport_be.exception.FieldError;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Standard error response body returned by the API for every rejected
 * request, as defined in the design document's error handling section.
 *
 * <pre>
 * {
 *   "timestamp": "2025-01-01T10:00:00Z",
 *   "status": 400,
 *   "errorCode": "PRODUCT_NAME_REQUIRED",
 *   "message": "Tên sản phẩm là bắt buộc.",
 *   "fieldErrors": [
 *     { "field": "name", "message": "Không được để trống" }
 *   ]
 * }
 * </pre>
 *
 * @param timestamp   the instant the error occurred
 * @param status      the HTTP status code of the response
 * @param errorCode   a machine-readable error code identifying the failure
 * @param message     a human-readable description of the error
 * @param fieldErrors field-level validation errors, empty when not applicable
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        List<FieldError> fieldErrors
) {

    public ErrorResponse(Instant timestamp, int status, String errorCode, String message, List<FieldError> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.fieldErrors = fieldErrors == null ? Collections.emptyList() : List.copyOf(fieldErrors);
    }

    /**
     * Convenience factory for errors without field-level validation details.
     */
    public static ErrorResponse of(int status, String errorCode, String message) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, Collections.emptyList());
    }

    /**
     * Convenience factory for validation errors that carry field-level details.
     */
    public static ErrorResponse of(int status, String errorCode, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, errorCode, message, fieldErrors);
    }
}
