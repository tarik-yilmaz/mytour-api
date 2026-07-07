package org.fhtw.mytourapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.fhtw.mytourapi.dto.DemoDataResultDto;
import org.fhtw.mytourapi.exception.ApiExceptionHandler;
import org.fhtw.mytourapi.exception.ApiErrorResponseFactory;
import org.fhtw.mytourapi.service.DemoDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DemoDataControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    {
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Mock
    private DemoDataService demoDataService;

    @BeforeEach
    void setUp() {
        DemoDataController controller = new DemoDataController(demoDataService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler(new ApiErrorResponseFactory()))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(jsonMapper))
                .build();
    }

    @Test
    void seedDemoDataReturns201WithCounts() throws Exception {
        when(demoDataService.seedDemoData()).thenReturn(new DemoDataResultDto(3, 10));

        mockMvc.perform(post("/api/demo-data/seed"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdTourCount").value(3))
                .andExpect(jsonPath("$.createdLogCount").value(10));
    }
}
