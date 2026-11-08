package com.mikeychoi.calendarcalc.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request body for counting days between two dates (POST /v1/date/difference).
 *
 * Example:
 * { "startDate": "2026-11-07", "endDate": "2026-11-17" }
 *
 * The rule "startDate must not be after endDate" involves both fields; it is added
 * together with the difference endpoint.
 */
public record DateDifferenceRequest(

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate) {
}
