package com.mikeychoi.calendarcalc.service;

import com.mikeychoi.calendarcalc.dto.HolidayRequest;
import com.mikeychoi.calendarcalc.dto.HolidayResponse;
import com.mikeychoi.calendarcalc.dto.HolidayUpdateRequest;
import com.mikeychoi.calendarcalc.entity.Holiday;
import com.mikeychoi.calendarcalc.exception.DuplicateHolidayException;
import com.mikeychoi.calendarcalc.exception.ResourceNotFoundException;
import com.mikeychoi.calendarcalc.repository.HolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void getHolidayReturnsDtoWhenIdExists() {
        when(holidayRepository.findById(1L))
                .thenReturn(Optional.of(holiday(1L, LocalDate.of(2027, 12, 25), "Christmas")));

        assertThat(holidayService.getHoliday(1L))
                .isEqualTo(new HolidayResponse(1L, LocalDate.of(2027, 12, 25), "Christmas"));
    }

    @Test
    void getHolidayThrowsNotFoundForUnknownId() {
        when(holidayRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidayService.getHoliday(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Holiday with id 99 not found");
    }

    // ---------- create ----------

    private static final LocalDate CHRISTMAS = LocalDate.of(2027, 12, 25);

    @Test
    void createHolidaySavesTrimmedNameAndReturnsDtoWithGeneratedId() {
        when(holidayRepository.existsByDateAndNormalizedName(CHRISTMAS, "christmas")).thenReturn(false);
        when(holidayRepository.saveAndFlush(any(Holiday.class))).thenAnswer(invocation -> {
            Holiday toSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(toSave, "id", 1L); // simulate the database assigning an id
            return toSave;
        });

        HolidayResponse result = holidayService.createHoliday(new HolidayRequest(CHRISTMAS, "  Christmas "));

        assertThat(result).isEqualTo(new HolidayResponse(1L, CHRISTMAS, "Christmas"));
    }

    @Test
    void createHolidayRejectsDuplicateWithoutSaving() {
        // The service must look up the NORMALIZED name, so "  CHRISTMAS " matches "Christmas".
        when(holidayRepository.existsByDateAndNormalizedName(CHRISTMAS, "christmas")).thenReturn(true);

        assertThatThrownBy(() -> holidayService.createHoliday(new HolidayRequest(CHRISTMAS, "  CHRISTMAS ")))
                .isInstanceOf(DuplicateHolidayException.class)
                .hasMessage("A holiday named 'CHRISTMAS' already exists on 2027-12-25");
        verify(holidayRepository, never()).saveAndFlush(any());
    }

    @Test
    void createHolidayReportsDatabaseConstraintViolationAsDuplicate() {
        // Simulates two identical requests racing: the exists-check passed, but the DB constraint fired.
        when(holidayRepository.existsByDateAndNormalizedName(CHRISTMAS, "christmas")).thenReturn(false);
        when(holidayRepository.saveAndFlush(any(Holiday.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violated"));

        assertThatThrownBy(() -> holidayService.createHoliday(new HolidayRequest(CHRISTMAS, "Christmas")))
                .isInstanceOf(DuplicateHolidayException.class);
    }

    // ---------- update ----------

    @Test
    void updateHolidayChangesOnlyTheSuppliedName() {
        when(holidayRepository.findById(1L)).thenReturn(Optional.of(holiday(1L, CHRISTMAS, "Christmas")));
        when(holidayRepository.existsByDateAndNormalizedNameAndIdNot(CHRISTMAS, "christmas day", 1L))
                .thenReturn(false);
        when(holidayRepository.saveAndFlush(any(Holiday.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HolidayResponse result = holidayService.updateHoliday(1L, new HolidayUpdateRequest(null, "Christmas Day"));

        assertThat(result).isEqualTo(new HolidayResponse(1L, CHRISTMAS, "Christmas Day"));
    }

    @Test
    void updateHolidayChangesOnlyTheSuppliedDate() {
        LocalDate observed = LocalDate.of(2027, 12, 24);
        when(holidayRepository.findById(1L)).thenReturn(Optional.of(holiday(1L, CHRISTMAS, "Christmas")));
        when(holidayRepository.existsByDateAndNormalizedNameAndIdNot(observed, "christmas", 1L))
                .thenReturn(false);
        when(holidayRepository.saveAndFlush(any(Holiday.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HolidayResponse result = holidayService.updateHoliday(1L, new HolidayUpdateRequest(observed, null));

        assertThat(result).isEqualTo(new HolidayResponse(1L, observed, "Christmas"));
    }

    @Test
    void updateHolidayThrowsNotFoundForUnknownId() {
        when(holidayRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> holidayService.updateHoliday(99L, new HolidayUpdateRequest(null, "X")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Holiday with id 99 not found");
        verify(holidayRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateHolidayRejectsChangeThatWouldDuplicateAnotherHoliday() {
        Holiday existing = holiday(2L, CHRISTMAS, "Company Shutdown");
        when(holidayRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(holidayRepository.existsByDateAndNormalizedNameAndIdNot(CHRISTMAS, "christmas", 2L))
                .thenReturn(true);

        assertThatThrownBy(() -> holidayService.updateHoliday(2L, new HolidayUpdateRequest(null, "Christmas")))
                .isInstanceOf(DuplicateHolidayException.class);
        verify(holidayRepository, never()).saveAndFlush(any());
        assertThat(existing.getName()).isEqualTo("Company Shutdown"); // entity left untouched
    }

    // ---------- delete ----------

    @Test
    void deleteHolidayDeletesExistingHoliday() {
        when(holidayRepository.existsById(1L)).thenReturn(true);

        holidayService.deleteHoliday(1L);

        verify(holidayRepository).deleteById(1L);
    }

    @Test
    void deleteHolidayThrowsNotFoundForUnknownId() {
        when(holidayRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> holidayService.deleteHoliday(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(holidayRepository, never()).deleteById(any());
    }

    /** Builds an entity with an id, as if it had been loaded from the database. */
    private static Holiday holiday(Long id, LocalDate date, String name) {
        Holiday holiday = new Holiday(date, name);
        ReflectionTestUtils.setField(holiday, "id", id); // id has no setter: only the DB assigns it
        return holiday;
    }
}
