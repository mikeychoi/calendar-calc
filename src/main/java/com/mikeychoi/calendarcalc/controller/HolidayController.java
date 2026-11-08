package com.mikeychoi.calendarcalc.controller;

import com.mikeychoi.calendarcalc.dto.HolidayResponse;
import com.mikeychoi.calendarcalc.service.HolidayService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HTTP endpoints for the holiday calendar.
 * Translates HTTP requests into service calls; contains no business or database logic.
 */
@RestController
@RequestMapping("/v1/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    /**
     * GET /v1/holidays            -> all holidays
     * GET /v1/holidays?year=2027  -> holidays in 2027
     *
     * Always 200 OK with a JSON array (possibly empty).
     */
    @GetMapping
    public List<HolidayResponse> getHolidays(
            @RequestParam(required = false) @Min(1) @Max(9999) Integer year) {
        return holidayService.getHolidays(year);
    }

    /**
     * GET /v1/holidays/{id}  -> one holiday
     *
     * 200 OK with the holiday, 404 if the id doesn't exist, 400 if id isn't a number.
     */
    @GetMapping("/{id}")
    public HolidayResponse getHoliday(@PathVariable Long id) {
        return holidayService.getHoliday(id);
    }
}
