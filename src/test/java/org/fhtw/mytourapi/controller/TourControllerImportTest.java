package org.fhtw.mytourapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.exception.ApiErrorResponseFactory;
import org.fhtw.mytourapi.exception.ApiExceptionHandler;
import org.fhtw.mytourapi.service.CoverImageStorageService;
import org.fhtw.mytourapi.service.IntermediateTourLogService;
import org.fhtw.mytourapi.service.IntermediateTourSearchIndex;
import org.fhtw.mytourapi.service.IntermediateTourService;
import org.fhtw.mytourapi.service.LocationSuggestionService;
import org.fhtw.mytourapi.service.RouteCalculationService;
import org.fhtw.mytourapi.service.TimezoneSuggestionService;
import org.fhtw.mytourapi.service.TourAttributeCalculator;
import org.fhtw.mytourapi.service.TourExportService;
import org.fhtw.mytourapi.service.TourImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TourControllerImportTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @TempDir
    private Path tempDirectory;

    private TourExportService exportService;
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
        exportService = new TourExportService(tourService, tourLogService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TourController(
                        tourService,
                        exportService,
                        new TourImportService(tourService, tourLogService),
                        new LocationSuggestionService(new OpenRouteServiceProperties(), (query, limit) -> List.of()),
                        new TimezoneSuggestionService()
                ))
                .setControllerAdvice(new ApiExceptionHandler(new ApiErrorResponseFactory()))
                .build();
    }

    @Test
    void importToursReturnsCreatedResult() throws Exception {
        mockMvc.perform(post("/api/tours/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportService.exportTours())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importedTours").value(4))
                .andExpect(jsonPath("$.importedLogs").value(9))
                .andExpect(jsonPath("$.createdTourIds.length()").value(4))
                .andExpect(jsonPath("$.createdTourIds[0]").value(5));
    }

    @Test
    void importToursReturnsStructuredValidationErrors() throws Exception {
        ObjectNode exportJson = objectMapper.valueToTree(exportService.exportTours());
        exportJson.put("schemaVersion", 99);

        mockMvc.perform(post("/api/tours/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportJson)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Import validation failed"))
                .andExpect(jsonPath("$.validationErrors[0]", containsString("schemaVersion")));
    }

    private RouteCalculationService routeCalculationService() {
        return new RouteCalculationService(
                new OpenRouteServiceProperties(),
                (profile, startCoordinate, endCoordinate, fetchedAt) -> {
                    throw new AssertionError("OpenRouteService client must not be used during import tests.");
                }
        );
    }

    private CoverImageStorageService coverImageStorageService() {
        ImageStorageProperties properties = new ImageStorageProperties();
        properties.setBaseDirectory(tempDirectory);
        return new CoverImageStorageService(properties);
    }
}
