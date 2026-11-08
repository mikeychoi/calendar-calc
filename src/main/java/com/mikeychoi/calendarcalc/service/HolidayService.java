package com.mikeychoi.calendarcalc.service;

import com.mikeychoi.calendarcalc.dto.HolidayResponse;
import com.mikeychoi.calendarcalc.entity.Holiday;
import com.mikeychoi.calendarcalc.repository.HolidayRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for the holiday calendar.
 * Knows nothing about HTTP: it takes plain Java values and returns DTOs.
 */
@Service
public class HolidayService {

    private final HolidayRepository holidayRepository;

    // Constructor injection: Spring sees this constructor and passes in the repository bean.
    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    /**
     * Returns holidays ordered by date.
     *
     * @param year if null, every holiday is returned; otherwise only holidays from
     *             January 1 to December 31 (inclusive) of that year
     */
    public List<HolidayResponse> getHolidays(Integer year) {
        List<Holiday> holidays;
        if (year == null) {
            holidays = holidayRepository.findAllByOrderByDateAsc();
        } else {
            holidays = holidayRepository.findByDateBetweenOrderByDateAsc(
                    LocalDate.of(year, 1, 1),
                    LocalDate.of(year, 12, 31));
        }

        return holidays.stream()
                .map(HolidayResponse::from)
                .toList();
    }
}
