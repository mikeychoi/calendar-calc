package com.mikeychoi.calendarcalc.controller;

import com.mikeychoi.calendarcalc.dto.HolidayRequest;
import com.mikeychoi.calendarcalc.dto.HolidayResponse;
import com.mikeychoi.calendarcalc.dto.HolidayUpdateRequest;
import com.mikeychoi.calendarcalc.exception.DuplicateHolidayException;
import com.mikeychoi.calendarcalc.exception.ResourceNotFoundException;
import com.mikeychoi.calendarcalc.service.HolidayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: starts only Spring MVC (controllers, JSON conversion,
 * @RestControllerAdvice), not the service or database. MockMvc sends simulated
 * HTTP requests without opening a real port. The service is replaced with a mock.
 */
@WebMvcTest(HolidayController.class)
class HolidayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HolidayService holidayService;

    private static final LocalDate CHRISTMAS = LocalDate.of(2027, 12, 25);

    // ---------- POST ----------

    @Test
    void postCreatesHolidayAndReturns201WithLocationHeader() throws Exception {
        when(holidayService.createHoliday(new HolidayRequest(CHRISTMAS, "Christmas")))
                .thenReturn(new HolidayResponse(7L, CHRISTMAS, "Christmas"));

        mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2027-12-25", "name": "Christmas" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/v1/holidays/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.date").value("2027-12-25"))
                .andExpect(jsonPath("$.name").value("Christmas"));
    }

    @Test
    void postWithMissingFieldsReturns400ListingEachProblem() throws Exception {
        mockMvc.perform(post("/v1/holidays").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("date must not be null; name must not be blank"))
                .andExpect(jsonPath("$.path").value("/v1/holidays"));

        verify(holidayService, never()).createHoliday(any());
    }

    @Test
    void postWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2027-12-25", "name": "   " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name must not be blank"));
    }

    @Test
    void postWithImpossibleDateReturns400NamingTheField() throws Exception {
        mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2027-13-45", "name": "Christmas" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid value for field 'date': dates must use the YYYY-MM-DD format"));
    }

    @Test
    void postWithNonIsoDateFormatReturns400() throws Exception {
        mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "12-25-2027", "name": "Christmas" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid value for field 'date': dates must use the YYYY-MM-DD format"));
    }

    @Test
    void postWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"date\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void postDuplicateReturns409() throws Exception {
        when(holidayService.createHoliday(any()))
                .thenThrow(new DuplicateHolidayException("A holiday named 'Christmas' already exists on 2027-12-25"));

        mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2027-12-25", "name": "Christmas" }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A holiday named 'Christmas' already exists on 2027-12-25"));
    }

    // ---------- PATCH ----------

    @Test
    void patchPassesOnlySuppliedFieldsAndReturnsUpdatedHoliday() throws Exception {
        // "date" is absent from the JSON, so it arrives as null = "leave unchanged".
        when(holidayService.updateHoliday(1L, new HolidayUpdateRequest(null, "Christmas Day")))
                .thenReturn(new HolidayResponse(1L, CHRISTMAS, "Christmas Day"));

        mockMvc.perform(patch("/v1/holidays/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Christmas Day" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.date").value("2027-12-25"))
                .andExpect(jsonPath("$.name").value("Christmas Day"));
    }

    @Test
    void patchWithBlankNameReturns400() throws Exception {
        mockMvc.perform(patch("/v1/holidays/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "  " }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name must not be blank"));

        verify(holidayService, never()).updateHoliday(anyLong(), any());
    }

    @Test
    void patchUnknownIdReturns404() throws Exception {
        when(holidayService.updateHoliday(any(), any()))
                .thenThrow(new ResourceNotFoundException("Holiday with id 99 not found"));

        mockMvc.perform(patch("/v1/holidays/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "X" }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Holiday with id 99 not found"));
    }

    @Test
    void patchThatWouldCreateDuplicateReturns409() throws Exception {
        when(holidayService.updateHoliday(any(), any()))
                .thenThrow(new DuplicateHolidayException("A holiday named 'Christmas' already exists on 2027-12-25"));

        mockMvc.perform(patch("/v1/holidays/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Christmas" }
                                """))
                .andExpect(status().isConflict());
    }

    // ---------- DELETE ----------

    @Test
    void deleteReturns204WithEmptyBody() throws Exception {
        mockMvc.perform(delete("/v1/holidays/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(holidayService).deleteHoliday(1L);
    }

    @Test
    void deleteUnknownIdReturns404() throws Exception {
        // deleteHoliday returns void, so we use doThrow(...).when(...) instead of when(...).thenThrow(...)
        doThrow(new ResourceNotFoundException("Holiday with id 99 not found"))
                .when(holidayService).deleteHoliday(99L);

        mockMvc.perform(delete("/v1/holidays/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Holiday with id 99 not found"));
    }

    // ---------- GET ----------

    @Test
    void getWithoutYearReturnsAllHolidaysAsJsonArray() throws Exception {
        when(holidayService.getHolidays(null)).thenReturn(List.of(
                new HolidayResponse(1L, LocalDate.of(2027, 12, 25), "Christmas"),
                new HolidayResponse(2L, LocalDate.of(2028, 1, 1), "New Year's Day")));

        mockMvc.perform(get("/v1/holidays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].date").value("2027-12-25")) // ISO format, not an array of numbers
                .andExpect(jsonPath("$[0].name").value("Christmas"))
                .andExpect(jsonPath("$[1].date").value("2028-01-01"));
    }

    @Test
    void getWithYearPassesYearToService() throws Exception {
        when(holidayService.getHolidays(2027)).thenReturn(List.of(
                new HolidayResponse(1L, LocalDate.of(2027, 12, 25), "Christmas")));

        mockMvc.perform(get("/v1/holidays").param("year", "2027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Christmas"));
    }

    @Test
    void getReturnsEmptyArrayWhenNoHolidays() throws Exception {
        when(holidayService.getHolidays(null)).thenReturn(List.of());

        mockMvc.perform(get("/v1/holidays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void nonNumericYearReturns400WithStandardErrorBody() throws Exception {
        mockMvc.perform(get("/v1/holidays").param("year", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("year must be a valid integer"))
                .andExpect(jsonPath("$.path").value("/v1/holidays"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(holidayService, never()).getHolidays(any());
    }

    @Test
    void yearBelowRangeReturns400() throws Exception {
        mockMvc.perform(get("/v1/holidays").param("year", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("year must be greater than or equal to 1"));

        verify(holidayService, never()).getHolidays(any());
    }

    @Test
    void getByIdReturnsHoliday() throws Exception {
        when(holidayService.getHoliday(1L))
                .thenReturn(new HolidayResponse(1L, LocalDate.of(2027, 12, 25), "Christmas"));

        mockMvc.perform(get("/v1/holidays/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.date").value("2027-12-25"))
                .andExpect(jsonPath("$.name").value("Christmas"));
    }

    @Test
    void getByUnknownIdReturns404WithStandardErrorBody() throws Exception {
        when(holidayService.getHoliday(99L))
                .thenThrow(new ResourceNotFoundException("Holiday with id 99 not found"));

        mockMvc.perform(get("/v1/holidays/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Holiday with id 99 not found"))
                .andExpect(jsonPath("$.path").value("/v1/holidays/99"));
    }

    @Test
    void getByNonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/v1/holidays/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be a valid integer"));

        verify(holidayService, never()).getHoliday(any());
    }

    @Test
    void yearAboveRangeReturns400() throws Exception {
        mockMvc.perform(get("/v1/holidays").param("year", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("year must be less than or equal to 9999"));

        verify(holidayService, never()).getHolidays(any());
    }
}
