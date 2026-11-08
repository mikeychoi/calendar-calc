package com.mikeychoi.calendarcalc.dto;

import java.time.LocalDate;

/**
 * JSON representation of a holiday returned by the API
 *
 * Example:
 * { "id": 1, "date": "2027-12-25", "name": "Christmas" }
 *
 */
public record HolidayResponse(Long id, LocalDate date, String name) {
}
