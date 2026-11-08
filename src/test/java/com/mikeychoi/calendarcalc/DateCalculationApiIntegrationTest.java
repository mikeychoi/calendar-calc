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
}
