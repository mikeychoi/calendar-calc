package com.mikeychoi.calendarcalc.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request body for adding or subtracting days
 * (POST /v1/date/add and POST /v1/date/subtract share this shape).
 *
 * Example:
 * { "startDate": "2026-11-07", "numDays": 10, "calcType": "CALENDAR" }
 */
public record DateAddSubtractRequest(

        @NotNull
        LocalDate startDate,

        // Integer (not int) so a missing value arrives as null and is rejected,
        // instead of silently defaulting to 0.
        @NotNull
        @Min(0)
        @Max(100_000)
        Integer numDays,

        @NotNull
        CalculationType calcType) {
}
