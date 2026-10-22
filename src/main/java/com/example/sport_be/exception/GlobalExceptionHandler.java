package com.example.sport_be.exception;

import com.example.sport_be.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

/**
 * Centralized exception handler that converts every business exception
 * (and a handful of framework-level exceptions) into the standard
 * {@link ErrorResponse} JSON shape, with the HTTP status mapping defined
 * in the design document's error handling section.
 *
 * <table>
 *   <caption>Error classification</caption>
 *   <tr><th>Exception</th><th>HTTP status</th></tr>
 *   <tr><td>{@link ValidationException} / {@link MethodArgumentNotValidException}</td><td>400 Bad Request</td></tr>
 *   <tr><td>{@link ResourceNotFoundException}</td><td>404 Not Found</td></tr>
 *   <tr><td>{@link ConflictException} / {@link DataIntegrityViolationException}</td><td>409 Conflict</td></tr>
 *   <tr><td>{@link BusinessRuleException}</td><td>422 Unprocessable Entity</td></tr>
 *   <tr><td>{@link UnauthorizedException}</td><td>401 Unauthorized</td></tr>
 *   <tr><td>{@link ForbiddenException}</td><td>403 Forbidden</td></tr>
 *   <tr><td>Any other {@link Exception}</td><td>500 Internal Server Error</td></tr>
 * </table>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String DATA_INTEGRITY_ERROR_CODE = "DATA_INTEGRITY_VIOLATION";
    private static final String INTERNAL_ERROR_CODE = "INTERNAL_SERVER_ERROR";

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getFieldErrors()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ValidationException.DEFAULT_ERROR_CODE,
                "Dữ liệu gửi lên không hợp lệ.",
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.CONFLICT.value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "IMAGE_TOO_LARGE",
                "Kích thước tệp tải lên vượt quá giới hạn cho phép."
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                DATA_INTEGRITY_ERROR_CODE,
                "Dữ liệu bị trùng hoặc vi phạm ràng buộc duy nhất."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED.value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        ErrorResponse body = ErrorResponse.of(HttpStatus.FORBIDDEN.value(), ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse body = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                INTERNAL_ERROR_CODE,
                "Đã có lỗi không mong đợi xảy ra. Vui lòng thử lại sau."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
