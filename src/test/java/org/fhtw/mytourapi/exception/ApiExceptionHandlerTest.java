package org.fhtw.mytourapi.exception;

import org.fhtw.mytourapi.controller.TourController;
import org.fhtw.mytourapi.service.IntermediateTourService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TourController(new IntermediateTourService()))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void missingTourReturnsStructuredApiError() throws Exception {
        mockMvc.perform(get("/api/tours/{tourId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Tour not found"))
                .andExpect(jsonPath("$.path").value("/api/tours/999"))
                .andExpect(jsonPath("$.validationErrors", empty()));
    }

    @Test
    void invalidTourRequestReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "description": "Validation test",
                                  "startLocation": "",
                                  "endLocation": "",
                                  "transportType": null,
                                  "timezoneId": "",
                                  "startCoordinate": null,
                                  "endCoordinate": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/tours"))
                .andExpect(jsonPath("$.validationErrors", hasItem(containsString("name"))))
                .andExpect(jsonPath("$.validationErrors", hasItem(containsString("startLocation"))))
                .andExpect(jsonPath("$.validationErrors", hasItem(containsString("transportType"))));
    }
}
