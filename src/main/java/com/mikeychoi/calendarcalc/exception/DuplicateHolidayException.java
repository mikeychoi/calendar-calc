package com.mikeychoi.calendarcalc.exception;

/**
 * Thrown when creating or updating a holiday would produce an exact duplicate:
 * same date and same name (ignoring capitalization and surrounding spaces).
 * GlobalExceptionHandler turns it into a 409 Conflict response.
 */
public class DuplicateHolidayException extends RuntimeException {

    public DuplicateHolidayException(String message) {
        super(message);
    }
}
