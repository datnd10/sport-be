package com.example.sport_be.exception;

/**
 * Represents a single field-level validation error, used to populate the
 * {@code fieldErrors} list of the standard error response DTO.
 *
 * @param field   the name of the field that failed validation
 * @param message a human-readable description of why the field is invalid
 */
public record FieldError(String field, String message) {
}
