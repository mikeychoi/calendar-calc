package com.mikeychoi.calendarcalc.exception;

/**
 * Thrown when a date range is not acceptable, e.g. startDate is after endDate
 * in a difference calculation. Mapped to 400 Bad Request.
 */
public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(String message) {
        super(message);
    }
}
