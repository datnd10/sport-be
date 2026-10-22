package com.example.sport_be.exception;

/**
 * Thrown when a request is well-formed but violates a business rule
 * (e.g. invalid order status transition, expired discount code, customer
 * not eligible to review a product). Maps to HTTP 422 Unprocessable
 * Entity in the GlobalExceptionHandler.
 */
public class BusinessRuleException extends BaseBusinessException {

    public static final String DEFAULT_ERROR_CODE = "BUSINESS_RULE_VIOLATION";

    public BusinessRuleException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
}
