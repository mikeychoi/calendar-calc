package com.mikeychoi.calendarcalc.service;

import com.mikeychoi.calendarcalc.dto.CalculationType;
import com.mikeychoi.calendarcalc.dto.DateAddSubtractRequest;
import com.mikeychoi.calendarcalc.dto.DateDifferenceRequest;
import com.mikeychoi.calendarcalc.dto.DateDifferenceResponse;
import com.mikeychoi.calendarcalc.entity.Holiday;
import com.mikeychoi.calendarcalc.exception.InvalidDateRangeException;
import com.mikeychoi.calendarcalc.repository.HolidayRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static com.mikeychoi.calendarcalc.dto.CalculationType.BUSINESS;
import static com.mikeychoi.calendarcalc.dto.CalculationType.CALENDAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for date calculations. The holiday repository is mocked, so each test
 * decides exactly which holidays exist.
 *
 * Reference weekdays used below (verified with java.time):
 *   2026-11-07 Sat | 2027-01-08 Fri, 01-10 Sun, 01-11 Mon
 *   2027-11-24 Wed, 11-25 Thu (Thanksgiving), 11-26 Fri, 11-29 Mon
 *   2027-12-24 Fri, 12-25 Sat (Christmas falls on a weekend), 12-27 Mon
 */
@ExtendWith(MockitoExtension.class)
class DateCalculationServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @InjectMocks
    private DateCalculationService service;

    private static final LocalDate THANKSGIVING = LocalDate.of(2027, 11, 25);
    private static final LocalDate DAY_AFTER_THANKSGIVING = LocalDate.of(2027, 11, 26);
    private static final LocalDate CHRISTMAS = LocalDate.of(2027, 12, 25);

    // ---------------------------------------------------------------- helpers

    private LocalDate add(String start, int numDays, CalculationType type) {
        return service.add(new DateAddSubtractRequest(LocalDate.parse(start), numDays, type)).resultDate();
    }

    private LocalDate subtract(String start, int numDays, CalculationType type) {
        return service.subtract(new DateAddSubtractRequest(LocalDate.parse(start), numDays, type)).resultDate();
    }

    private void givenHolidays(LocalDate... dates) {
        when(holidayRepository.findAll()).thenReturn(
                java.util.Arrays.stream(dates).map(d -> new Holiday(d, "Holiday " + d)).toList());
    }

    // ---------------------------------------------------------------- calendar days

    @Nested
    class CalendarDays {

        @Test
        void addPositiveDays() {
            assertThat(add("2026-11-07", 10, CALENDAR)).isEqualTo("2026-11-17");
        }

        @Test
        void addZeroDaysReturnsStartDate() {
            assertThat(add("2026-11-07", 0, CALENDAR)).isEqualTo("2026-11-07");
        }

        @Test
        void addAcrossMonthBoundary() {
            assertThat(add("2027-01-28", 5, CALENDAR)).isEqualTo("2027-02-02");
        }

        @Test
        void addAcrossYearBoundary() {
            assertThat(add("2026-12-30", 5, CALENDAR)).isEqualTo("2027-01-04");
        }

        @Test
        void addAcrossLeapDay() {
            // 2028 is a leap year: Feb 27 -> 28 -> 29 -> Mar 1
            assertThat(add("2028-02-27", 3, CALENDAR)).isEqualTo("2028-03-01");
            // 2027 is not: Feb 27 -> 28 -> Mar 1 -> Mar 2
            assertThat(add("2027-02-27", 3, CALENDAR)).isEqualTo("2027-03-02");
        }

        @Test
        void subtractPositiveDays() {
            assertThat(subtract("2026-11-17", 10, CALENDAR)).isEqualTo("2026-11-07");
        }

        @Test
        void subtractAcrossYearBoundary() {
            assertThat(subtract("2027-01-02", 5, CALENDAR)).isEqualTo("2026-12-28");
        }

        @Test
        void calendarCalculationsIgnoreHolidaysAndNeverQueryThem() {
            add("2027-11-24", 1, CALENDAR);
            subtract("2027-11-24", 1, CALENDAR);

            verifyNoInteractions(holidayRepository);
        }

        @Test
        void responseEchoesInputs() {
            var response = service.add(new DateAddSubtractRequest(LocalDate.of(2026, 11, 7), 10, CALENDAR));

            assertThat(response.startDate()).isEqualTo("2026-11-07");
            assertThat(response.resultDate()).isEqualTo("2026-11-17");
            assertThat(response.numDays()).isEqualTo(10);
            assertThat(response.calcType()).isEqualTo(CALENDAR);
        }
    }

    // ---------------------------------------------------------------- business days

    @Nested
    class BusinessDays {

        @Test
        void oneBusinessDayAfterFridayIsMonday() {
            givenHolidays();
            assertThat(add("2027-01-08", 1, BUSINESS)).isEqualTo("2027-01-11");
        }

        @Test
        void crossingAWeekendSkipsSaturdayAndSunday() {
            givenHolidays();
            // Thu -> Fri(1), [Sat, Sun skipped], Mon(2), Tue(3)
            assertThat(add("2027-01-07", 3, BUSINESS)).isEqualTo("2027-01-12");
        }

        @Test
        void startingOnAWeekendCountsFromTheNextWeekday() {
            givenHolidays();
            // Spec example: Sat 2026-11-07 + 10 business days = Mon 9..Fri 13 (5), Mon 16..Fri 20 (10)
            assertThat(add("2026-11-07", 10, BUSINESS)).isEqualTo("2026-11-20");
        }

        @Test
        void zeroBusinessDaysReturnsStartDateEvenOnAWeekend() {
            // No stubbing: zero days must not even load holidays (strict stubs would flag unused ones).
            assertThat(add("2026-11-07", 0, BUSINESS)).isEqualTo("2026-11-07");
            verifyNoInteractions(holidayRepository);
        }

        @Test
        void crossingAHolidaySkipsIt() {
            givenHolidays(THANKSGIVING);
            // Wed -> [Thu holiday] -> Fri
            assertThat(add("2027-11-24", 1, BUSINESS)).isEqualTo("2027-11-26");
        }

        @Test
        void consecutiveHolidaysAndAWeekendAreAllSkipped() {
            givenHolidays(THANKSGIVING, DAY_AFTER_THANKSGIVING);
            // Wed -> [Thu holiday, Fri holiday, Sat, Sun] -> Mon
            assertThat(add("2027-11-24", 1, BUSINESS)).isEqualTo("2027-11-29");
        }

        @Test
        void multipleHolidaysOnTheSameDateAreSkippedOnce() {
            when(holidayRepository.findAll()).thenReturn(List.of(
                    new Holiday(THANKSGIVING, "Thanksgiving"),
                    new Holiday(THANKSGIVING, "Company Holiday")));
            // Wed -> [Thu holiday] -> Fri(1) -> [weekend] -> Mon(2)
            assertThat(add("2027-11-24", 2, BUSINESS)).isEqualTo("2027-11-29");
        }

        @Test
        void holidayOnAWeekendHasNoExtraEffect() {
            givenHolidays(CHRISTMAS); // Saturday
            // Fri -> [Sat (weekend and holiday), Sun] -> Mon: same as with no holiday
            assertThat(add("2027-12-24", 1, BUSINESS)).isEqualTo("2027-12-27");
        }

        @Test
        void subtractOneBusinessDayFromMondayIsFriday() {
            givenHolidays();
            assertThat(subtract("2027-01-11", 1, BUSINESS)).isEqualTo("2027-01-08");
        }

        @Test
        void subtractStartingOnAWeekend() {
            givenHolidays();
            // Sun -> [Sat] -> Fri
            assertThat(subtract("2027-01-10", 1, BUSINESS)).isEqualTo("2027-01-08");
        }

        @Test
        void subtractSkipsConsecutiveHolidaysAndWeekend() {
            givenHolidays(THANKSGIVING, DAY_AFTER_THANKSGIVING);
            // Mon -> [Sun, Sat, Fri holiday, Thu holiday] -> Wed
            assertThat(subtract("2027-11-29", 1, BUSINESS)).isEqualTo("2027-11-24");
        }
    }

    // ---------------------------------------------------------------- difference

    @Nested
    class Difference {

        private DateDifferenceResponse difference(String start, String end) {
            return service.difference(new DateDifferenceRequest(LocalDate.parse(start), LocalDate.parse(end)));
        }

        private void givenHolidaysBetween(String from, String to, LocalDate... dates) {
            when(holidayRepository.findByDateBetweenOrderByDateAsc(LocalDate.parse(from), LocalDate.parse(to)))
                    .thenReturn(java.util.Arrays.stream(dates).map(d -> new Holiday(d, "Holiday " + d)).toList());
        }

        @Test
        void specExampleWithoutHolidays() {
            // Interval (Sat Nov 7, Tue Nov 17]: holidays are looked up from Nov 8 (start excluded)
            givenHolidaysBetween("2026-11-08", "2026-11-17");

            DateDifferenceResponse result = difference("2026-11-07", "2026-11-17");

            assertThat(result.calendarDays()).isEqualTo(10);
            assertThat(result.businessDays()).isEqualTo(7);
            assertThat(result.startDate()).isEqualTo("2026-11-07");
            assertThat(result.endDate()).isEqualTo("2026-11-17");
        }

        @Test
        void startDateIsExcludedAndEndDateIncluded() {
            givenHolidaysBetween("2027-01-09", "2027-01-11");
            // (Fri, Mon]: Sat, Sun, Mon -> 3 calendar days, 1 business day (Fri itself not counted)
            DateDifferenceResponse result = difference("2027-01-08", "2027-01-11");

            assertThat(result.calendarDays()).isEqualTo(3);
            assertThat(result.businessDays()).isEqualTo(1);
        }

        @Test
        void sameStartAndEndDateIsZero() {
            givenHolidaysBetween("2027-01-09", "2027-01-08"); // empty range
            DateDifferenceResponse result = difference("2027-01-08", "2027-01-08");

            assertThat(result.calendarDays()).isZero();
            assertThat(result.businessDays()).isZero();
        }

        @Test
        void holidaysInTheIntervalReduceBusinessDaysOnly() {
            givenHolidaysBetween("2027-11-23", "2027-11-29", THANKSGIVING, DAY_AFTER_THANKSGIVING);
            // (Mon 22, Mon 29]: Tue, Wed, [Thu, Fri holidays], [Sat, Sun], Mon -> 3 business days
            DateDifferenceResponse result = difference("2027-11-22", "2027-11-29");

            assertThat(result.calendarDays()).isEqualTo(7);
            assertThat(result.businessDays()).isEqualTo(3);
        }

        @Test
        void multipleHolidaysOnTheSameDateAreCountedOnce() {
            when(holidayRepository.findByDateBetweenOrderByDateAsc(LocalDate.of(2027, 11, 23), LocalDate.of(2027, 11, 29)))
                    .thenReturn(List.of(new Holiday(THANKSGIVING, "Thanksgiving"),
                                        new Holiday(THANKSGIVING, "Company Holiday")));
            // Tue, Wed, [Thu], Fri, Mon -> 4 (not 3)
            assertThat(difference("2027-11-22", "2027-11-29").businessDays()).isEqualTo(4);
        }

        @Test
        void holidayOnAWeekendDoesNotReduceBusinessDays() {
            givenHolidaysBetween("2027-12-21", "2027-12-27", CHRISTMAS);
            // (Mon 20, Mon 27]: Tue, Wed, Thu, Fri, [Sat = Christmas], [Sun], Mon -> 5
            assertThat(difference("2027-12-20", "2027-12-27").businessDays()).isEqualTo(5);
        }

        @Test
        void startAfterEndIsRejected() {
            assertThatThrownBy(() -> difference("2026-11-17", "2026-11-07"))
                    .isInstanceOf(InvalidDateRangeException.class)
                    .hasMessage("startDate must be on or before endDate");
            verifyNoInteractions(holidayRepository);
        }

        @Test
        void rangeLongerThanLimitIsRejected() {
            LocalDate start = LocalDate.of(2000, 1, 1);
            LocalDate end = start.plusDays(DateCalculationService.MAX_RANGE_DAYS + 1);

            assertThatThrownBy(() -> service.difference(new DateDifferenceRequest(start, end)))
                    .isInstanceOf(InvalidDateRangeException.class)
                    .hasMessage("Date range must not exceed 100000 days");
        }
    }
}
