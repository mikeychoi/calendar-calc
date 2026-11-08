package com.mikeychoi.calendarcalc;

import com.mikeychoi.calendarcalc.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for date calculations: real controller, service, repository and
 * in-memory database. Holidays are created through the holiday API, proving that
 * business-day calculations actually read the holiday calendar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DateCalculationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HolidayRepository holidayRepository;

    @BeforeEach
    void startWithEmptyCalendar() {
        holidayRepository.deleteAll();
    }

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void businessDayAddSkipsHolidaysStoredInTheCalendar() throws Exception {
        // Wed 2027-11-24 + 1 business day = Thu 11-25 with no holidays...
        postJson("/v1/date/add", """
                { "startDate": "2027-11-24", "numDays": 1, "calcType": "BUSINESS" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultDate").value("2027-11-25"));

        // ...but after adding Thanksgiving (Thu) and the day after (Fri), it becomes Mon 11-29.
        postJson("/v1/holidays", """
                { "date": "2027-11-25", "name": "Thanksgiving" }
                """).andExpect(status().isCreated());
        postJson("/v1/holidays", """
                { "date": "2027-11-26", "name": "Day after Thanksgiving" }
                """).andExpect(status().isCreated());

        postJson("/v1/date/add", """
                { "startDate": "2027-11-24", "numDays": 1, "calcType": "BUSINESS" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultDate").value("2027-11-29"));

        // Calendar days ignore holidays entirely.
        postJson("/v1/date/add", """
                { "startDate": "2027-11-24", "numDays": 1, "calcType": "CALENDAR" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultDate").value("2027-11-25"));
    }

    @Test
    void addingPastTheLargestSupportedDateReturns400() throws Exception {
        // LocalDate.MAX is +999999999-12-31; one more day cannot be represented.
        postJson("/v1/date/add", """
                { "startDate": "+999999999-12-31", "numDays": 1, "calcType": "CALENDAR" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The resulting date is outside the supported date range"));
    }

    @Test
    void businessDaySubtractSkipsHolidaysStoredInTheCalendar() throws Exception {
        // Mon 2027-11-29 - 1 business day = Fri 11-26 with no holidays...
        postJson("/v1/date/subtract", """
                { "startDate": "2027-11-29", "numDays": 1, "calcType": "BUSINESS" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultDate").value("2027-11-26"));

        // ...but with Thu 11-25 and Fri 11-26 as holidays, it moves back to Wed 11-24.
        postJson("/v1/holidays", """
                { "date": "2027-11-25", "name": "Thanksgiving" }
                """).andExpect(status().isCreated());
        postJson("/v1/holidays", """
                { "date": "2027-11-26", "name": "Day after Thanksgiving" }
                """).andExpect(status().isCreated());

        postJson("/v1/date/subtract", """
                { "startDate": "2027-11-29", "numDays": 1, "calcType": "BUSINESS" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultDate").value("2027-11-24"));
    }

    @Test
    void subtractingPastTheSmallestSupportedDateReturns400() throws Exception {
        // LocalDate.MIN is -999999999-01-01; one day earlier cannot be represented.
        postJson("/v1/date/subtract", """
                { "startDate": "-999999999-01-01", "numDays": 1, "calcType": "CALENDAR" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The resulting date is outside the supported date range"));
    }

    @Test
    void differenceMatchesTheSpecExample() throws Exception {
        postJson("/v1/date/difference", """
                { "startDate": "2026-11-07", "endDate": "2026-11-17" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calendarDays").value(10))
                .andExpect(jsonPath("$.businessDays").value(7));
    }

    @Test
    void differenceSubtractsHolidaysStoredInTheCalendarFromBusinessDaysOnly() throws Exception {
        String request = """
                { "startDate": "2027-11-22", "endDate": "2027-11-29" }
                """;

        // (Mon 11-22, Mon 11-29]: Tue, Wed, Thu, Fri, Mon = 5 business days
        postJson("/v1/date/difference", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calendarDays").value(7))
                .andExpect(jsonPath("$.businessDays").value(5));

        // Thu and Fri become holidays. The second Thursday record shares a date, so it changes nothing.
        postJson("/v1/holidays", """
                { "date": "2027-11-25", "name": "Thanksgiving" }
                """).andExpect(status().isCreated());
        postJson("/v1/holidays", """
                { "date": "2027-11-26", "name": "Day after Thanksgiving" }
                """).andExpect(status().isCreated());
        postJson("/v1/holidays", """
                { "date": "2027-11-25", "name": "Company Holiday" }
                """).andExpect(status().isCreated());

        postJson("/v1/date/difference", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calendarDays").value(7))
                .andExpect(jsonPath("$.businessDays").value(3));
    }

    @Test
    void differenceWithSameStartAndEndDateIsZero() throws Exception {
        postJson("/v1/date/difference", """
                { "startDate": "2027-01-08", "endDate": "2027-01-08" }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calendarDays").value(0))
                .andExpect(jsonPath("$.businessDays").value(0));
    }

    @Test
    void differenceWithReversedDatesReturns400() throws Exception {
        postJson("/v1/date/difference", """
                { "startDate": "2026-11-17", "endDate": "2026-11-07" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startDate must be on or before endDate"));
    }

    @Test
    void differenceRangeOverTheLimitReturns400() throws Exception {
        // 2000-01-01 to 2300-01-01 is about 109,573 days, over the 100,000-day limit.
        postJson("/v1/date/difference", """
                { "startDate": "2000-01-01", "endDate": "2300-01-01" }
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Date range must not exceed 100000 days"));
    }
}
