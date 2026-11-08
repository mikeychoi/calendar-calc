package com.mikeychoi.calendarcalc.exception;

import com.mikeychoi.calendarcalc.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Converts exceptions thrown anywhere in a controller call into the standard ApiError JSON.
 *
 * Specific handlers cover known errors (400, 404, 409); a final catch-all covers Spring's
 * own client errors (404 unknown URL, 405, 415, ...) and unexpected failures (500).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // SLF4J is the logging API Spring Boot uses; output goes to the console by default.
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    /**
     * A calculation produced a date java.time can't represent (beyond year +/-999,999,999),
     * e.g. adding days to "+999999999-12-31". Caused by the input, so 400 rather than 500.
     */
    @ExceptionHandler(DateTimeException.class)
    public ResponseEntity<ApiError> handleDateOutOfRange(
            DateTimeException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "The resulting date is outside the supported date range", request);
    }

    /**
     * Catch-all for every exception not handled by a more specific method above.
     * (Spring always picks the most specific matching @ExceptionHandler, so this never
     * overrides the 400/404/409 handlers.)
     *
     * Two kinds of exception end up here:
     * 1. Spring MVC's own client errors (unknown URL, unsupported HTTP method, unsupported
     *    Content-Type, ...). These implement ErrorResponse, which carries the correct status,
     *    a safe message, and any required headers (e.g. "Allow" for 405). We keep all of that
     *    and only change the body to our ApiError format.
     * 2. Anything else is a bug or an infrastructure failure (e.g. the database is down).
     *    The full details are logged for developers; the client gets a generic 500 with no
     *    stack trace, SQL, or class names.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            if (status.is5xxServerError()) {
                log.error("Server error handling {} {}", request.getMethod(), request.getRequestURI(), ex);
            }
            return build(status, describeSpringError(springError, status, request), request,
                    springError.getHeaders());
        }

        log.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        return build(status, message, request, HttpHeaders.EMPTY);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
                                           HttpHeaders headers) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).headers(headers).body(body);
    }

    private static String describeSpringError(ErrorResponse springError, HttpStatus status,
                                              HttpServletRequest request) {
        // Spring's own 404 text mentions "static resource", which is an internal detail.
        if (springError instanceof NoResourceFoundException) {
            return "No endpoint found for " + request.getMethod() + " " + request.getRequestURI();
        }
        String detail = springError.getBody().getDetail();
        return detail != null ? detail : status.getReasonPhrase();
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
                if (jacksonEx instanceof MismatchedInputException mismatch && mismatch.getTargetType() != null) {
                    Class<?> target = mismatch.getTargetType();
                    if (target == LocalDate.class) {
                        message += ": dates must use the YYYY-MM-DD format";
                    } else if (target.isEnum()) {
                        // e.g. "must be one of CALENDAR, BUSINESS" - listed from the enum itself,
                        // so the message stays correct if values are added later.
                        message += ": must be one of " + Arrays.stream(target.getEnumConstants())
                                .map(Object::toString)
                                .collect(Collectors.joining(", "));
                    }
                }
                return message;
            }
        }
        return "Request body is missing or is not valid JSON";
    }
}
