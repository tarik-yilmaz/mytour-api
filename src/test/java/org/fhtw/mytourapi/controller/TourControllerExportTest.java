package org.fhtw.mytourapi.controller;

import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.exception.ApiErrorResponseFactory;
import org.fhtw.mytourapi.exception.ApiExceptionHandler;
import org.fhtw.mytourapi.service.CoverImageStorageService;
import org.fhtw.mytourapi.service.IntermediateTourLogService;
import org.fhtw.mytourapi.service.IntermediateTourSearchIndex;
import org.fhtw.mytourapi.service.IntermediateTourService;
import org.fhtw.mytourapi.service.RouteCalculationService;
import org.fhtw.mytourapi.service.TourAttributeCalculator;
import org.fhtw.mytourapi.service.TourExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TourControllerExportTest {

    @TempDir
    private Path tempDirectory;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        IntermediateTourSearchIndex tourSearchIndex = new IntermediateTourSearchIndex();
        IntermediateTourService tourService = new IntermediateTourService(
                routeCalculationService(),
                coverImageStorageService(),
                new TourAttributeCalculator(),
                tourSearchIndex
        );
        IntermediateTourLogService tourLogService = new IntermediateTourLogService(tourService, tourSearchIndex);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TourController(tourService, new TourExportService(tourService, tourLogService)))
                .setControllerAdvice(new ApiExceptionHandler(new ApiErrorResponseFactory()))
                .build();
    }

    @Test
    void exportToursReturnsJsonExportPayload() throws Exception {
        mockMvc.perform(get("/api/tours/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value(1))
                .andExpect(jsonPath("$.exportedAt").isNotEmpty())
                .andExpect(jsonPath("$.tours.length()").value(4))
                .andExpect(jsonPath("$.tours[0].tour.name").value("Danube Island Evening Ride"))
                .andExpect(jsonPath("$.tours[0].tour.route.routeSource").value("OPENROUTESERVICE"))
                .andExpect(jsonPath("$.tours[0].logs.length()").value(3))
                .andExpect(jsonPath("$.tours[0].logs[0].weather.provider").value("OPEN_METEO"))
                .andExpect(jsonPath("$.tours[0].logs[0].weather.weatherDescription").value("clear sky"));
    }

    private RouteCalculationService routeCalculationService() {
        return new RouteCalculationService(
                new OpenRouteServiceProperties(),
                (profile, startCoordinate, endCoordinate, fetchedAt) -> {
                    throw new AssertionError("OpenRouteService client must not be used without an API key.");
                }
        );
    }

    private CoverImageStorageService coverImageStorageService() {
        ImageStorageProperties properties = new ImageStorageProperties();
        properties.setBaseDirectory(tempDirectory);
        return new CoverImageStorageService(properties);
    }
}
