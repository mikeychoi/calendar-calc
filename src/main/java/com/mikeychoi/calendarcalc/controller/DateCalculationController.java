package com.mikeychoi.calendarcalc.controller;

import com.mikeychoi.calendarcalc.dto.DateAddSubtractRequest;
import com.mikeychoi.calendarcalc.dto.DateAddSubtractResponse;
import com.mikeychoi.calendarcalc.service.DateCalculationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP endpoints for date calculations.
 * Translates HTTP requests into service calls; the calculation rules live in DateCalculationService.
 *
 * These are POST endpoints even though nothing is stored: each one sends a structured
 * JSON input and gets a computed result back, and the spec defines JSON request bodies.
 */
@RestController
@RequestMapping("/v1/date")
public class DateCalculationController {

    private final DateCalculationService dateCalculationService;

    public DateCalculationController(DateCalculationService dateCalculationService) {
        this.dateCalculationService = dateCalculationService;
    }

    /**
     * POST /v1/date/add  -> add calendar or business days to a date
     *
     * 200 OK with the result date.
     */
    @PostMapping("/add")
    public DateAddSubtractResponse add(@Valid @RequestBody DateAddSubtractRequest request) {
        return dateCalculationService.add(request);
    }

    /**
     * POST /v1/date/subtract  -> subtract calendar or business days from a date
     *
     * 200 OK with the result date.
     */
    @PostMapping("/subtract")
    public DateAddSubtractResponse subtract(@Valid @RequestBody DateAddSubtractRequest request) {
        return dateCalculationService.subtract(request);
    }
}
