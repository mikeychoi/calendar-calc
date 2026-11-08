package com.mikeychoi.calendarcalc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for creating a holiday (POST /v1/holidays). Both fields are required.
 *
 * Example:
 * { "date": "2027-12-25", "name": "Christmas" }
 *
 * The validation annotations are checked when the controller parameter is marked @Valid.
 */
public record HolidayRequest(

        @NotNull
        LocalDate date,

        @NotBlank
        @Size(max = 255) // matches the database column length
        String name) {
}
