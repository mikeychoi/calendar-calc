package com.mikeychoi.calendarcalc.exception;

import com.mikeychoi.calendarcalc.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Converts exceptions thrown anywhere in a controller call into the standard ApiError JSON.
 *
 * Covers the errors the holiday endpoints can produce. A catch-all for unexpected
 * exceptions (500) comes in the dedicated error-handling step.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** A URL value can't be converted to the parameter's type, e.g. ?year=abc for an Integer. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = ex.getName() + " must be a valid " + describeType(ex.getRequiredType());
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** A parameter failed a validation annotation, e.g. @Min(1) on ?year=0. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleParameterValidation(
            HandlerMethodValidationException ex, HttpServletRequest request) {
        // Produces e.g. "year must be greater than or equal to 1"
        String message = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName()
                                + " " + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** A @Valid request body failed validation, e.g. a missing date or a blank name. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Produces e.g. "date must not be null; name must not be blank" (sorted, so the order is stable)
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * The request body couldn't be turned into the DTO: missing body, broken JSON,
     * or a value of the wrong type/format, e.g. "date": "2027-13-45".
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, describeUnreadableBody(ex), request);
    }

    /** The service couldn't find the requested record, e.g. GET /v1/holidays/999. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** Creating or updating would produce an exact duplicate holiday. */
    @ExceptionHandler(DuplicateHolidayException.class)
    public ResponseEntity<ApiError> handleDuplicate(
            DuplicateHolidayException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    private static String describeType(Class<?> type) {
        if (type == Integer.class || type == int.class || type == Long.class || type == long.class) {
            return "integer";
        }
        return type == null ? "value" : type.getSimpleName();
    }

    /**
     * Builds a helpful message without exposing parser internals. Jackson (the JSON library)
     * records which field it was reading when it failed; we use that to name the field.
     */
    private static String describeUnreadableBody(HttpMessageNotReadableException ex) {
        // Walk the cause chain to find Jackson's exception, if there is one.
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof JacksonException jacksonEx) {
                String field = jacksonEx.getPath().stream()
                        .map(JacksonException.Reference::getPropertyName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("."));
                if (field.isEmpty()) {
                    break; // the JSON itself is broken, not a particular field
                }
                String message = "Invalid value for field '" + field + "'";
                if (jacksonEx instanceof MismatchedInputException mismatch
                        && mismatch.getTargetType() == LocalDate.class) {
                    message += ": dates must use the YYYY-MM-DD format";
                }
                return message;
            }
        }
        return "Request body is missing or is not valid JSON";
    }
}
