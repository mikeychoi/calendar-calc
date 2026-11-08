package com.mikeychoi.calendarcalc.dto;

import com.mikeychoi.calendarcalc.entity.Holiday;

import java.time.LocalDate;

/**
 * JSON representation of a holiday returned by the API
 *
 * Example:
 * { "id": 1, "date": "2027-12-25", "name": "Christmas" }
 *
 */
public record HolidayResponse(Long id, LocalDate date, String name) {

    /** Copies only the public fields from the entity (normalizedName stays internal). */
    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(holiday.getId(), holiday.getDate(), holiday.getName());
    }
}
