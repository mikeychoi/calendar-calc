package com.mikeychoi.calendarcalc.exception;

import com.mikeychoi.calendarcalc.controller.HolidayController;
import com.mikeychoi.calendarcalc.service.HolidayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the catch-all behaviour of GlobalExceptionHandler: unexpected failures and
 * Spring MVC's own client errors must all use the standard ApiError format.
 * (The specific 400/404/409 handlers are covered in HolidayControllerTest.)
 */
@WebMvcTest(HolidayController.class)
@ExtendWith(OutputCaptureExtension.class) // lets a test read what was logged
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HolidayService holidayService;

    @Test
    void unexpectedExceptionReturnsGeneric500AndLogsDetailsOnServer(CapturedOutput output) throws Exception {
        when(holidayService.getHolidays(null))
                .thenThrow(new IllegalStateException("database connection pool exhausted"));

        mockMvc.perform(get("/v1/holidays"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.path").value("/v1/holidays"))
                // internal details must not leak to the client
                .andExpect(content().string(not(containsString("database connection pool"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));

        // ...but they are logged for developers, including the stack trace
        assertThat(output)
                .contains("Unexpected error handling GET /v1/holidays")
                .contains("java.lang.IllegalStateException: database connection pool exhausted");
    }

    @Test
    void unknownPathReturns404InStandardFormat() throws Exception {
        mockMvc.perform(get("/v1/nonsense"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No endpoint found for GET /v1/nonsense"))
                .andExpect(jsonPath("$.path").value("/v1/nonsense"));
    }

    @Test
    void unsupportedHttpMethodReturns405WithAllowHeader() throws Exception {
        // PUT isn't supported on /v1/holidays/{id} (we use PATCH for updates)
        mockMvc.perform(put("/v1/holidays/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("PATCH")))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").value(containsString("PUT")));
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/v1/holidays")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("Christmas"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.path").value("/v1/holidays"));
    }
}
