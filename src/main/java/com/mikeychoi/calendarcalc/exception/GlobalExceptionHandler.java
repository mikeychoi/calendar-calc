package com.mikeychoi.calendarcalc.exception;

import com.mikeychoi.calendarcalc.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Converts exceptions thrown anywhere in a controller call into the standard ApiError JSON.
 *
 * Minimal for now: only the cases the GET endpoints can produce.
 * More handlers (invalid JSON body, duplicates, unexpected errors) come later.
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

    /** The service couldn't find the requested record, e.g. GET /v1/holidays/999. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
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
}
