package com.mikeychoi.calendarcalc.service;

import com.mikeychoi.calendarcalc.dto.HolidayRequest;
import com.mikeychoi.calendarcalc.dto.HolidayResponse;
import com.mikeychoi.calendarcalc.dto.HolidayUpdateRequest;
import com.mikeychoi.calendarcalc.entity.Holiday;
import com.mikeychoi.calendarcalc.exception.DuplicateHolidayException;
import com.mikeychoi.calendarcalc.exception.ResourceNotFoundException;
import com.mikeychoi.calendarcalc.repository.HolidayRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Returns one holiday by id.
     *
     * @throws ResourceNotFoundException if no holiday has that id
     */
    public HolidayResponse getHoliday(Long id) {
        return HolidayResponse.from(findHolidayOrThrow(id));
    }

    /**
     * Creates a holiday.
     *
     * @throws DuplicateHolidayException if a holiday with the same date and name already exists
     */
    @Transactional
    public HolidayResponse createHoliday(HolidayRequest request) {
        if (holidayRepository.existsByDateAndNormalizedName(
                request.date(), Holiday.normalizeName(request.name()))) {
            throw duplicate(request.date(), request.name());
        }

        Holiday saved = saveOrThrowDuplicate(new Holiday(request.date(), request.name()));
        return HolidayResponse.from(saved);
    }

    /**
     * Partially updates a holiday: only non-null fields in the request are changed.
     *
     * @throws ResourceNotFoundException if no holiday has that id
     * @throws DuplicateHolidayException if the change would duplicate another holiday
     */
    @Transactional
    public HolidayResponse updateHoliday(Long id, HolidayUpdateRequest request) {
        Holiday holiday = findHolidayOrThrow(id);

        // Work out the final values first, keeping the current value for anything not supplied.
        LocalDate newDate = request.date() != null ? request.date() : holiday.getDate();
        String newName = request.name() != null ? request.name() : holiday.getName();

        // Check for duplicates BEFORE modifying the entity. Once a managed entity is changed,
        // Hibernate may write the change to the database before running the next query.
        if (holidayRepository.existsByDateAndNormalizedNameAndIdNot(
                newDate, Holiday.normalizeName(newName), id)) {
            throw duplicate(newDate, newName);
        }

        holiday.setDate(newDate);
        holiday.setName(newName);
        return HolidayResponse.from(saveOrThrowDuplicate(holiday));
    }

    /**
     * Deletes a holiday.
     *
     * @throws ResourceNotFoundException if no holiday has that id
     */
    @Transactional
    public void deleteHoliday(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw notFound(id);
        }
        holidayRepository.deleteById(id);
    }

    private Holiday findHolidayOrThrow(Long id) {
        return holidayRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    /**
     * Saves immediately (flush) so a unique-constraint violation surfaces here rather than at
     * commit time. That only happens if two identical requests race past the exists-check at
     * the same moment; the database constraint is the backstop, reported as the same 409.
     */
    private Holiday saveOrThrowDuplicate(Holiday holiday) {
        try {
            return holidayRepository.saveAndFlush(holiday);
        } catch (DataIntegrityViolationException ex) {
            throw duplicate(holiday.getDate(), holiday.getName());
        }
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Holiday with id " + id + " not found");
    }

    private static DuplicateHolidayException duplicate(LocalDate date, String name) {
        return new DuplicateHolidayException(
                "A holiday named '" + name.trim() + "' already exists on " + date);
    }
}
