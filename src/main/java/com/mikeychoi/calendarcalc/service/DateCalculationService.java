package com.mikeychoi.calendarcalc.service;

import com.mikeychoi.calendarcalc.dto.DateAddSubtractRequest;
import com.mikeychoi.calendarcalc.dto.DateAddSubtractResponse;
import com.mikeychoi.calendarcalc.dto.DateDifferenceRequest;
import com.mikeychoi.calendarcalc.dto.DateDifferenceResponse;
import com.mikeychoi.calendarcalc.entity.Holiday;
import com.mikeychoi.calendarcalc.exception.InvalidDateRangeException;
import com.mikeychoi.calendarcalc.repository.HolidayRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calendar-day and business-day calculations.
 *
 * Conventions:
 * - Calendar days: every day counts.
 * - Business days: Monday-Friday, excluding dates in the holiday calendar.
 * - Add/subtract: the start date itself is never counted; zero days returns the start date.
 * - Difference: counts days in the interval (startDate, endDate] - start excluded, end included.
 */
@Service
public class DateCalculationService {

    /** Largest difference range accepted, matching the numDays limit for add/subtract. */
    static final long MAX_RANGE_DAYS = 100000;

    private static final int FORWARD = 1;
    private static final int BACKWARD = -1;

    private final HolidayRepository holidayRepository;

    public DateCalculationService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    public DateAddSubtractResponse add(DateAddSubtractRequest request) {
        return shift(request, FORWARD);
    }

    public DateAddSubtractResponse subtract(DateAddSubtractRequest request) {
        return shift(request, BACKWARD);
    }

    /**
     * Counts calendar days and business days in (startDate, endDate].
     *
     * @throws InvalidDateRangeException if startDate is after endDate, or the range is too long
     */
    public DateDifferenceResponse difference(DateDifferenceRequest request) {
        LocalDate start = request.startDate();
        LocalDate end = request.endDate();

        if (start.isAfter(end)) {
            throw new InvalidDateRangeException("startDate must be on or before endDate");
        }
        long calendarDays = ChronoUnit.DAYS.between(start, end);
        if (calendarDays > MAX_RANGE_DAYS) {
            throw new InvalidDateRangeException("Date range must not exceed " + MAX_RANGE_DAYS + " days");
        }

        // Only holidays inside the interval matter; start + 1 because the start date is excluded.
        Set<LocalDate> holidays = toDateSet(
                holidayRepository.findByDateBetweenOrderByDateAsc(start.plusDays(1), end));

        // datesUntil's upper bound is exclusive, so end + 1 includes the end date.
        long businessDays = start.plusDays(1)
                .datesUntil(end.plusDays(1))
                .filter(date -> isBusinessDay(date, holidays))
                .count();

        return new DateDifferenceResponse(start, end, calendarDays, businessDays);
    }

    /** Shared by add (direction +1) and subtract (direction -1). */
    private DateAddSubtractResponse shift(DateAddSubtractRequest request, int direction) {
        LocalDate start = request.startDate();
        int numDays = request.numDays();

        LocalDate result = switch (request.calcType()) {
            case CALENDAR -> start.plusDays((long) direction * numDays);
            case BUSINESS -> shiftBusinessDays(start, numDays, direction);
        };

        return new DateAddSubtractResponse(start, result, numDays, request.calcType());
    }

    /**
     *  step one calendar day at a time in the given direction, and count the step only 
     *  if it lands on a business day.
     */
    private LocalDate shiftBusinessDays(LocalDate start, int numDays, int direction) {
        if (numDays == 0) {
            return start; 
        }

        // load holidays
        Set<LocalDate> holidays = toDateSet(holidayRepository.findAll());

        LocalDate date = start;
        int counted = 0;
        while (counted < numDays) {
            date = date.plusDays(direction);
            if (isBusinessDay(date, holidays)) {
                counted++;
            }
        }
        return date;
    }

    private static boolean isBusinessDay(LocalDate date, Set<LocalDate> holidays) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY
                && day != DayOfWeek.SUNDAY
                && !holidays.contains(date);
    }

    /** Several holidays may share a date; a Set keeps each date once. */
    private static Set<LocalDate> toDateSet(List<Holiday> holidays) {
        return holidays.stream()
                .map(Holiday::getDate)
                .collect(Collectors.toSet());
    }
}
