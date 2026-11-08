package com.mikeychoi.calendarcalc.dto;

import java.time.LocalDate;

/**
 * Response for add/subtract calculations: echoes the inputs alongside the result,
 * so the client can see exactly what was calculated.
 *
 * Example:
 * { "startDate": "2026-11-07", "resultDate": "2026-11-17", "numDays": 10, "calcType": "CALENDAR" }
 */
public record DateAddSubtractResponse(
        LocalDate startDate,
        LocalDate resultDate,
        int numDays,
        CalculationType calcType) {
}
