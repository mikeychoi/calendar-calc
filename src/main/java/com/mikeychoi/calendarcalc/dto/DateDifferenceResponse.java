package com.mikeychoi.calendarcalc.dto;

import java.time.LocalDate;

/**
 * Response for the difference calculation. Both counts use the interval
 * (startDate, endDate]: the start date is excluded and the end date is included.
 *
 * Example (no holidays in the interval):
 * { "startDate": "2026-11-07", "endDate": "2026-11-17", "calendarDays": 10, "businessDays": 7 }
 */
public record DateDifferenceResponse(
        LocalDate startDate,
        LocalDate endDate,
        long calendarDays, // long matches ChronoUnit.DAYS.between(...), which returns long
        long businessDays) {
}
