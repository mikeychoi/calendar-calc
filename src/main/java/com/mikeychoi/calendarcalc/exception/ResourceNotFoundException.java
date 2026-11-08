package com.mikeychoi.calendarcalc.exception;

/**
 * Thrown by the service layer when a requested record does not exist.
 * GlobalExceptionHandler turns it into a 404 Not Found response, so services
 * never deal with HTTP status codes themselves.
 *
 * Extends RuntimeException (unchecked) so callers aren't forced to catch or
 * declare it; it simply propagates up to the exception handler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
