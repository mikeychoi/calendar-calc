package com.mikeychoi.calendarcalc.controller;

import com.mikeychoi.calendarcalc.dto.HolidayRequest;
import com.mikeychoi.calendarcalc.dto.HolidayResponse;
import com.mikeychoi.calendarcalc.dto.HolidayUpdateRequest;
import com.mikeychoi.calendarcalc.service.HolidayService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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

    /**
     * POST /v1/holidays  -> create a holiday
     *
     * 201 Created with the new holiday and a Location header pointing to it.
     */
    @PostMapping
    public ResponseEntity<HolidayResponse> createHoliday(@Valid @RequestBody HolidayRequest request) {
        HolidayResponse created = holidayService.createHoliday(request);

        // Builds e.g. http://localhost:8080/v1/holidays/7 from the current request URL.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    /**
     * PATCH /v1/holidays/{id}  -> partially update a holiday
     *
     * 200 OK with the updated holiday.
     */
    @PatchMapping("/{id}")
    public HolidayResponse updateHoliday(@PathVariable Long id,
                                         @Valid @RequestBody HolidayUpdateRequest request) {
        return holidayService.updateHoliday(id, request);
    }

    /**
     * DELETE /v1/holidays/{id}  -> remove a holiday
     *
     * 204 No Content (empty body) on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.noContent().build();
    }
}
