package com.mikeychoi.calendarcalc.service;

import com.mikeychoi.calendarcalc.dto.HolidayResponse;
import com.mikeychoi.calendarcalc.entity.Holiday;
import com.mikeychoi.calendarcalc.repository.HolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/**
 * Pure unit test: no Spring context, no database.
 * The repository is a Mockito mock, so we control exactly what it returns
 * and can verify which query the service chose.
 */
@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private HolidayService holidayService;

    @Test
    void withoutYearReturnsAllHolidaysAsDtos() {
        when(holidayRepository.findAllByOrderByDateAsc()).thenReturn(List.of(
                holiday(1L, LocalDate.of(2027, 12, 25), "Christmas"),
                holiday(2L, LocalDate.of(2028, 1, 1), "New Year's Day")));

        List<HolidayResponse> result = holidayService.getHolidays(null);

        assertThat(result).containsExactly(
                new HolidayResponse(1L, LocalDate.of(2027, 12, 25), "Christmas"),
                new HolidayResponse(2L, LocalDate.of(2028, 1, 1), "New Year's Day"));
        verify(holidayRepository, never()).findByDateBetweenOrderByDateAsc(any(), any());
    }

    @Test
    void withYearQueriesJanuaryFirstThroughDecemberThirtyFirst() {
        when(holidayRepository.findByDateBetweenOrderByDateAsc(
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)))
                .thenReturn(List.of(holiday(1L, LocalDate.of(2027, 12, 25), "Christmas")));

        List<HolidayResponse> result = holidayService.getHolidays(2027);

        assertThat(result).containsExactly(
                new HolidayResponse(1L, LocalDate.of(2027, 12, 25), "Christmas"));
        verify(holidayRepository, never()).findAllByOrderByDateAsc();
    }

    @Test
    void returnsEmptyListWhenNoHolidaysExist() {
        when(holidayRepository.findAllByOrderByDateAsc()).thenReturn(List.of());

        assertThat(holidayService.getHolidays(null)).isEmpty();
    }

    /** Builds an entity with an id, as if it had been loaded from the database. */
    private static Holiday holiday(Long id, LocalDate date, String name) {
        Holiday holiday = new Holiday(date, name);
        ReflectionTestUtils.setField(holiday, "id", id); // id has no setter: only the DB assigns it
        return holiday;
    }
}
