package com.mikeychoi.calendarcalc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies springdoc generates API documentation from our controllers.
 * Starts the full application (like CalendarCalcApplicationTests) but uses
 * MockMvc instead of a real HTTP port.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsDescribeGetHolidaysWithOptionalYearParameter() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/v1/holidays'].get").exists())
                .andExpect(jsonPath("$.paths['/v1/holidays'].get.parameters[0].name").value("year"))
                .andExpect(jsonPath("$.paths['/v1/holidays'].get.parameters[0].required").value(false))
                // springdoc reads @Min/@Max from the controller and documents the allowed range
                .andExpect(jsonPath("$.paths['/v1/holidays'].get.parameters[0].schema.minimum").value(1))
                .andExpect(jsonPath("$.paths['/v1/holidays'].get.parameters[0].schema.maximum").value(9999));
    }

    @Test
    void swaggerUiPageIsServed() throws Exception {
        // /swagger-ui.html redirects to the actual UI page
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
