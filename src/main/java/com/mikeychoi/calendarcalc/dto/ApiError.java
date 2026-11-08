package com.mikeychoi.calendarcalc.dto;

import java.time.LocalDateTime;

/**
 * Standard JSON body for every error response.
 *
 * Example:
 * {
 *   "timestamp": "2026-11-07T10:30:00",
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "year must be a valid integer",
 *   "path": "/v1/holidays"
 * }
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path) {
}
