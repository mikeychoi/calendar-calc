package com.mikeychoi.calendarcalc.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for partially updating a holiday (PATCH /v1/holidays/{id}).
 *
 * Every field is optional: a missing or null field means "leave unchanged".
 *
 * Example (rename only):
 * { "name": "Christmas Day" }
 */
public record HolidayUpdateRequest(

        LocalDate date,

        // null is allowed (= not supplied), but a supplied name must contain a non-space character.
        // (@NotBlank can't be used here because it also rejects null.)
        @Pattern(regexp = ".*\\S.*", message = "must not be blank")
        @Size(max = 255)
        String name) {
}
