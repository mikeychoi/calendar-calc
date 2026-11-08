package com.mikeychoi.calendarcalc.controller;

import com.mikeychoi.calendarcalc.dto.DateAddSubtractRequest;
import com.mikeychoi.calendarcalc.dto.DateAddSubtractResponse;
import com.mikeychoi.calendarcalc.service.DateCalculationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.DateTimeException;
import java.time.LocalDate;

import static com.mikeychoi.calendarcalc.dto.CalculationType.BUSINESS;
import static com.mikeychoi.calendarcalc.dto.CalculationType.CALENDAR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the date-calculation endpoints: JSON in/out, status codes and
 * validation errors. The service is mocked; calculation rules are tested in
 * DateCalculationServiceTest.
 */
@WebMvcTest(DateCalculationController.class)
class DateCalculationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DateCalculationService dateCalculationService;

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Nested
    class Add {

        @Test
        void returnsCalculatedResult() throws Exception {
            when(dateCalculationService.add(new DateAddSubtractRequest(LocalDate.of(2026, 11, 7), 10, CALENDAR)))
                    .thenReturn(new DateAddSubtractResponse(
                            LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 17), 10, CALENDAR));

            postJson("/v1/date/add", """
                    { "startDate": "2026-11-07", "numDays": 10, "calcType": "CALENDAR" }
                    """)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.startDate").value("2026-11-07"))
                    .andExpect(jsonPath("$.resultDate").value("2026-11-17"))
                    .andExpect(jsonPath("$.numDays").value(10))
                    .andExpect(jsonPath("$.calcType").value("CALENDAR"));
        }

        @Test
        void acceptsBusinessCalcType() throws Exception {
            when(dateCalculationService.add(new DateAddSubtractRequest(LocalDate.of(2026, 11, 7), 10, BUSINESS)))
                    .thenReturn(new DateAddSubtractResponse(
                            LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 20), 10, BUSINESS));

            postJson("/v1/date/add", """
                    { "startDate": "2026-11-07", "numDays": 10, "calcType": "BUSINESS" }
                    """)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultDate").value("2026-11-20"))
                    .andExpect(jsonPath("$.calcType").value("BUSINESS"));
        }

        @Test
        void missingFieldsReturn400ListingEachProblem() throws Exception {
            postJson("/v1/date/add", "{}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            "calcType must not be null; numDays must not be null; startDate must not be null"))
                    .andExpect(jsonPath("$.path").value("/v1/date/add"));

            verify(dateCalculationService, never()).add(any());
        }

        @Test
        void negativeNumDaysReturns400() throws Exception {
            // Matches the example error in CLAUDE.md
            postJson("/v1/date/add", """
                    { "startDate": "2026-11-07", "numDays": -1, "calcType": "CALENDAR" }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("numDays must be greater than or equal to 0"));
        }

        @Test
        void numDaysAboveLimitReturns400() throws Exception {
            postJson("/v1/date/add", """
                    { "startDate": "2026-11-07", "numDays": 100001, "calcType": "CALENDAR" }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("numDays must be less than or equal to 100000"));
        }

        @Test
        void fractionalNumDaysReturns400() throws Exception {
            // Must be rejected, not silently truncated to 1
            postJson("/v1/date/add", """
                    { "startDate": "2026-11-07", "numDays": 1.5, "calcType": "CALENDAR" }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for field 'numDays'"));

            verify(dateCalculationService, never()).add(any());
        }

        @Test
        void invalidCalcTypeReturns400ListingAllowedValues() throws Exception {
            postJson("/v1/date/add", """
                    { "startDate": "2026-11-07", "numDays": 10, "calcType": "WEEKLY" }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for field 'calcType': must be one of CALENDAR, BUSINESS"));
        }

        @Test
        void invalidStartDateReturns400() throws Exception {
            postJson("/v1/date/add", """
                    { "startDate": "11-07-2026", "numDays": 10, "calcType": "CALENDAR" }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value("Invalid value for field 'startDate': dates must use the YYYY-MM-DD format"));
        }

        @Test
        void resultOutsideSupportedRangeReturns400() throws Exception {
            when(dateCalculationService.add(any()))
                    .thenThrow(new DateTimeException("Invalid value for EpochDay"));

            postJson("/v1/date/add", """
                    { "startDate": "2026-11-07", "numDays": 10, "calcType": "CALENDAR" }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("The resulting date is outside the supported date range"));
        }
    }

    /** Validation is shared with add (same DTO), so only the routing and one error case are repeated here. */
    @Nested
    class Subtract {

        @Test
        void returnsCalculatedResultFromSubtractNotAdd() throws Exception {
            when(dateCalculationService.subtract(new DateAddSubtractRequest(LocalDate.of(2026, 11, 17), 10, CALENDAR)))
                    .thenReturn(new DateAddSubtractResponse(
                            LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 7), 10, CALENDAR));

            postJson("/v1/date/subtract", """
                    { "startDate": "2026-11-17", "numDays": 10, "calcType": "CALENDAR" }
                    """)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.startDate").value("2026-11-17"))
                    .andExpect(jsonPath("$.resultDate").value("2026-11-07"))
                    .andExpect(jsonPath("$.numDays").value(10))
                    .andExpect(jsonPath("$.calcType").value("CALENDAR"));

            verify(dateCalculationService, never()).add(any());
        }

        @Test
        void acceptsBusinessCalcType() throws Exception {
            when(dateCalculationService.subtract(new DateAddSubtractRequest(LocalDate.of(2027, 1, 11), 1, BUSINESS)))
                    .thenReturn(new DateAddSubtractResponse(
                            LocalDate.of(2027, 1, 11), LocalDate.of(2027, 1, 8), 1, BUSINESS));

            postJson("/v1/date/subtract", """
                    { "startDate": "2027-01-11", "numDays": 1, "calcType": "BUSINESS" }
                    """)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultDate").value("2027-01-08"))
                    .andExpect(jsonPath("$.calcType").value("BUSINESS"));
        }

        @Test
        void negativeNumDaysReturns400WithSubtractPath() throws Exception {
            postJson("/v1/date/subtract", """
                    { "startDate": "2026-11-17", "numDays": -1, "calcType": "CALENDAR" }
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("numDays must be greater than or equal to 0"))
                    .andExpect(jsonPath("$.path").value("/v1/date/subtract"));

            verify(dateCalculationService, never()).subtract(any());
        }
    }
}
