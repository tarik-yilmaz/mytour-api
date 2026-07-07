package org.fhtw.mytourapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.fhtw.mytourapi.dto.CreateTourLogRequest;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.UpdateTourLogRequest;
import org.fhtw.mytourapi.exception.ApiExceptionHandler;
import org.fhtw.mytourapi.exception.ApiErrorResponseFactory;
import org.fhtw.mytourapi.service.TourLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TourLogControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    {
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Mock
    private TourLogService tourLogService;

    @BeforeEach
    void setUp() {
        TourLogController controller = new TourLogController(tourLogService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler(new ApiErrorResponseFactory()))
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(jsonMapper))
                .build();
    }

    @Test
    void listLogsReturns200WhenTourFound() throws Exception {
        when(tourLogService.listLogs(1L)).thenReturn(Optional.of(List.of(sampleLog(10L, 1L))));

        mockMvc.perform(get("/api/tours/1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].tourId").value(1));
    }

    @Test
    void listLogsReturns404WhenTourNotFound() throws Exception {
        when(tourLogService.listLogs(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tours/99/logs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Tour not found"));
    }

    @Test
    void createLogReturns201WithValidBody() throws Exception {
        when(tourLogService.createLog(eq(1L), any(CreateTourLogRequest.class)))
                .thenReturn(Optional.of(sampleLog(10L, 1L)));

        mockMvc.perform(post("/api/tours/1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateLogJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void createLogReturns400WhenDifficultyIsNull() throws Exception {
        String json = """
                {
                  "performedAt": "2026-06-21T10:00:00Z",
                  "comment": "Great tour",
                  "difficulty": null,
                  "totalDistanceM": 12000,
                  "totalTimeS": 3600,
                  "rating": 4
                }
                """;

        mockMvc.perform(post("/api/tours/1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLogReturns400WhenRatingExceedsMax() throws Exception {
        String json = """
                {
                  "performedAt": "2026-06-21T10:00:00Z",
                  "comment": "Great tour",
                  "difficulty": 3,
                  "totalDistanceM": 12000,
                  "totalTimeS": 3600,
                  "rating": 6
                }
                """;

        mockMvc.perform(post("/api/tours/1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLogReturns400WhenTotalTimeIsZero() throws Exception {
        String json = """
                {
                  "performedAt": "2026-06-21T10:00:00Z",
                  "comment": "Great tour",
                  "difficulty": 3,
                  "totalDistanceM": 12000,
                  "totalTimeS": 0,
                  "rating": 4
                }
                """;

        mockMvc.perform(post("/api/tours/1/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLogReturns404WhenTourNotFound() throws Exception {
        when(tourLogService.createLog(eq(99L), any(CreateTourLogRequest.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/tours/99/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateLogJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tour not found"));
    }

    @Test
    void getLogReturns200WhenFound() throws Exception {
        when(tourLogService.getLog(1L, 10L)).thenReturn(Optional.of(sampleLog(10L, 1L)));

        mockMvc.perform(get("/api/tours/1/logs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getLogReturns404WhenNotFound() throws Exception {
        when(tourLogService.getLog(1L, 99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tours/1/logs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tour log not found"));
    }

    @Test
    void updateLogReturns200WhenFound() throws Exception {
        when(tourLogService.updateLog(eq(1L), eq(10L), any(UpdateTourLogRequest.class)))
                .thenReturn(Optional.of(sampleLog(10L, 1L)));

        mockMvc.perform(put("/api/tours/1/logs/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateLogJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void updateLogReturns404WhenNotFound() throws Exception {
        when(tourLogService.updateLog(eq(1L), eq(99L), any(UpdateTourLogRequest.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/tours/1/logs/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateLogJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tour log not found"));
    }

    @Test
    void deleteLogReturns204WhenFound() throws Exception {
        when(tourLogService.deleteLog(1L, 10L)).thenReturn(true);

        mockMvc.perform(delete("/api/tours/1/logs/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLogReturns404WhenNotFound() throws Exception {
        when(tourLogService.deleteLog(1L, 99L)).thenReturn(false);

        mockMvc.perform(delete("/api/tours/1/logs/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void refreshWeatherReturns200WhenFound() throws Exception {
        TourLogWeatherDto weather = new TourLogWeatherDto(
                10L, "OPEN_METEO", "archive",
                new CoordinateDto(new BigDecimal("48.2"), new BigDecimal("16.3")),
                Instant.parse("2026-06-21T10:00:00Z"),
                new BigDecimal("20.5"), new BigDecimal("60"),
                new BigDecimal("0.0"), 3, "Cloudy",
                new BigDecimal("15.0"), Instant.parse("2026-06-21T11:00:00Z")
        );
        when(tourLogService.refreshWeather(1L, 10L)).thenReturn(Optional.of(weather));

        mockMvc.perform(post("/api/tours/1/logs/10/weather/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("OPEN_METEO"))
                .andExpect(jsonPath("$.temperatureC").value(20.5));
    }

    @Test
    void refreshWeatherReturns404WhenNotFound() throws Exception {
        when(tourLogService.refreshWeather(1L, 99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/tours/1/logs/99/weather/refresh"))
                .andExpect(status().isNotFound());
    }

    private TourLogDto sampleLog(Long id, Long tourId) {
        return new TourLogDto(
                id, tourId,
                Instant.parse("2026-06-21T10:00:00Z"),
                "Great tour", (short) 3,
                new BigDecimal("12000"), 3600, (short) 4,
                null,
                Instant.parse("2026-06-21T10:05:00Z"),
                Instant.parse("2026-06-21T10:05:00Z"),
                0L
        );
    }

    private String validCreateLogJson() {
        return """
                {
                  "performedAt": "2026-06-21T10:00:00Z",
                  "comment": "Great tour",
                  "difficulty": 3,
                  "totalDistanceM": 12000,
                  "totalTimeS": 3600,
                  "rating": 4
                }
                """;
    }

    private String validUpdateLogJson() {
        return """
                {
                  "performedAt": "2026-06-21T10:00:00Z",
                  "comment": "Updated comment",
                  "difficulty": 2,
                  "totalDistanceM": 11000,
                  "totalTimeS": 3300,
                  "rating": 5,
                  "version": 0
                }
                """;
    }
}
