package com.mikeychoi.calendarcalc;

import com.jayway.jsonpath.JsonPath;
import com.mikeychoi.calendarcalc.repository.HolidayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the holiday API: real controller, service, repository and
 * in-memory H2 database (nothing mocked). Complements the unit tests by proving
 * the layers work together, including transactions and the unique constraint.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HolidayApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HolidayRepository holidayRepository;

    @BeforeEach
    void startWithEmptyCalendar() {
        holidayRepository.deleteAll();
    }

    @Test
    void holidayLifecycle() throws Exception {
        // Create
        String createdJson = mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2027-12-25", "name": "  Christmas " }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Christmas"))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(createdJson, "$.id");

        // Read it back by id
        mockMvc.perform(get("/v1/holidays/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2027-12-25"))
                .andExpect(jsonPath("$.name").value("Christmas"));

        // Same date + same name (different case/spaces) is a duplicate
        mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2027-12-25", "name": "CHRISTMAS" }
                                """))
                .andExpect(status().isConflict());

        // A different holiday on the same date is allowed
        mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "date": "2027-12-25", "name": "Company Shutdown" }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/holidays").param("year", "2027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Partial update: only the name changes
        mockMvc.perform(patch("/v1/holidays/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Christmas Day" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Christmas Day"))
                .andExpect(jsonPath("$.date").value("2027-12-25"));

        // Delete, then it's gone
        mockMvc.perform(delete("/v1/holidays/" + id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/v1/holidays/" + id))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/v1/holidays/" + id))
                .andExpect(status().isNotFound());
    }
}
