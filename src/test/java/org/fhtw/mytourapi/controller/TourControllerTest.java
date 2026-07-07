package org.fhtw.mytourapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.dto.CreateTourRequest;
import org.fhtw.mytourapi.dto.ImportResultDto;
import org.fhtw.mytourapi.dto.LocationSuggestionDto;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourExportDto;
import org.fhtw.mytourapi.dto.TourImportRequest;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TourSearchResponse;
import org.fhtw.mytourapi.dto.TourSummaryDto;
import org.fhtw.mytourapi.dto.TourSuggestionDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.fhtw.mytourapi.dto.UpdateTourRequest;
import org.fhtw.mytourapi.exception.ApiExceptionHandler;
import org.fhtw.mytourapi.exception.ApiErrorResponseFactory;
import org.fhtw.mytourapi.exception.ConflictException;
import org.fhtw.mytourapi.exception.UpstreamServiceException;
import org.fhtw.mytourapi.service.LocationSuggestionService;
import org.fhtw.mytourapi.service.StoredCoverImage;
import org.fhtw.mytourapi.service.TimezoneSuggestionService;
import org.fhtw.mytourapi.service.TourExportService;
import org.fhtw.mytourapi.service.TourImportService;
import org.fhtw.mytourapi.service.TourService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TourControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    {
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Mock
    private TourService tourService;
    @Mock
    private TourExportService tourExportService;
    @Mock
    private TourImportService tourImportService;
    @Mock
    private LocationSuggestionService locationSuggestionService;
    @Mock
    private TimezoneSuggestionService timezoneSuggestionService;

    @BeforeEach
    void setUp() {
        TourController controller = new TourController(
                tourService,
                tourExportService,
                tourImportService,
                locationSuggestionService,
                timezoneSuggestionService
        );
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler(new ApiErrorResponseFactory()))
                .setValidator(validator)
                .setMessageConverters(
                        new ResourceHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(jsonMapper)
                )
                .build();
    }

    @Test
    void searchToursReturns200WithResults() throws Exception {
        TourSummaryDto summary = sampleSummary(1L, "Danube Bike Tour");
        when(tourService.searchTours(any(), any(), any(), any(), any()))
                .thenReturn(new TourSearchResponse(List.of(summary), 1));

        mockMvc.perform(get("/api/tours")
                        .param("q", "danube")
                        .param("transportType", "BIKE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.tours[0].id").value(1))
                .andExpect(jsonPath("$.tours[0].name").value("Danube Bike Tour"));
    }

    @Test
    void getTourReturns200WhenFound() throws Exception {
        when(tourService.getTour(1L)).thenReturn(Optional.of(sampleDetail(1L)));

        mockMvc.perform(get("/api/tours/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Tour"));
    }

    @Test
    void getTourReturns404WhenNotFound() throws Exception {
        when(tourService.getTour(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tours/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Tour not found"));
    }

    @Test
    void createTourReturns201WithValidBody() throws Exception {
        when(tourService.createTour(any(CreateTourRequest.class))).thenReturn(sampleDetail(1L));

        mockMvc.perform(post("/api/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateTourJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Tour"));
    }

    @Test
    void createTourReturns400WhenNameIsBlank() throws Exception {
        String json = """
                {
                  "name": "",
                  "description": "desc",
                  "startLocation": "Vienna",
                  "endLocation": "Graz",
                  "transportType": "BIKE",
                  "timezoneId": "Europe/Vienna",
                  "startCoordinate": {"latitude": 48.2, "longitude": 16.3},
                  "endCoordinate": {"latitude": 47.0, "longitude": 15.4}
                }
                """;

        mockMvc.perform(post("/api/tours")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateTourReturns200WhenFound() throws Exception {
        when(tourService.updateTour(eq(1L), any(UpdateTourRequest.class)))
                .thenReturn(Optional.of(sampleDetail(1L)));

        mockMvc.perform(put("/api/tours/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateTourJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateTourReturns404WhenNotFound() throws Exception {
        when(tourService.updateTour(eq(99L), any(UpdateTourRequest.class)))
                .thenReturn(Optional.empty());

        mockMvc.perform(put("/api/tours/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateTourJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tour not found"));
    }

    @Test
    void deleteTourReturns204WhenFound() throws Exception {
        when(tourService.deleteTour(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/tours/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteTourReturns404WhenNotFound() throws Exception {
        when(tourService.deleteTour(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/tours/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void refreshRouteReturns200WhenFound() throws Exception {
        TourRouteDto route = new TourRouteDto(
                "OPENROUTESERVICE", "cycling-regular",
                new CoordinateDto(new BigDecimal("48.2"), new BigDecimal("16.3")),
                new CoordinateDto(new BigDecimal("47.0"), new BigDecimal("15.4")),
                new CoordinateDto(new BigDecimal("47.6"), new BigDecimal("15.9")),
                null, Instant.parse("2026-06-21T10:00:00Z")
        );
        when(tourService.refreshRoute(1L)).thenReturn(Optional.of(route));

        mockMvc.perform(post("/api/tours/1/route/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeSource").value("OPENROUTESERVICE"));
    }

    @Test
    void refreshRouteReturns404WhenNotFound() throws Exception {
        when(tourService.refreshRoute(99L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/tours/99/route/refresh"))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadCoverImageReturns200WhenTourFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        CoverImageDto coverImage = new CoverImageDto("covers/abc-cover.jpg", "cover.jpg", "image/jpeg", 3L);
        when(tourService.uploadCoverImage(eq(1L), any())).thenReturn(Optional.of(coverImage));

        mockMvc.perform(multipart("/api/tours/1/cover-image")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("covers/abc-cover.jpg"))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    }

    @Test
    void uploadCoverImageReturns404WhenTourNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        when(tourService.uploadCoverImage(eq(99L), any())).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/api/tours/99/cover-image")
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCoverImageReturnsImageContentWhenFound() throws Exception {
        byte[] content = new byte[]{1, 2, 3};
        when(tourService.getCoverImage(1L)).thenReturn(Optional.of(new StoredCoverImage(
                new ByteArrayResource(content),
                "cover.jpg",
                MediaType.IMAGE_JPEG,
                content.length
        )));

        mockMvc.perform(get("/api/tours/1/cover-image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"cover.jpg\"; filename*=UTF-8''cover.jpg"
                ))
                .andExpect(content().bytes(content));
    }

    @Test
    void getCoverImageReturns404WhenNotFound() throws Exception {
        when(tourService.getCoverImage(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/tours/99/cover-image"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCoverImageReturns204WhenTourFound() throws Exception {
        when(tourService.deleteCoverImage(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/tours/1/cover-image"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCoverImageReturns404WhenTourNotFound() throws Exception {
        when(tourService.deleteCoverImage(99L)).thenReturn(false);

        mockMvc.perform(delete("/api/tours/99/cover-image"))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportToursReturns200() throws Exception {
        when(tourExportService.exportTours())
                .thenReturn(new TourExportDto(1, Instant.parse("2026-06-21T10:00:00Z"), List.of()));

        mockMvc.perform(get("/api/tours/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value(1))
                .andExpect(jsonPath("$.tours").isArray());
    }

    @Test
    void importToursReturns201OnSuccess() throws Exception {
        when(tourImportService.importTours(any(TourImportRequest.class)))
                .thenReturn(new ImportResultDto(2, 3, List.of(10L, 11L)));

        String json = """
                {
                  "schemaVersion": 1,
                  "tours": [
                    {
                      "tour": {
                        "name": "Imported Tour",
                        "description": "desc",
                        "startLocation": "A",
                        "endLocation": "B",
                        "transportType": "BIKE",
                        "timezoneId": "Europe/Vienna",
                        "startCoordinate": {"latitude": 48.2, "longitude": 16.3},
                        "endCoordinate": {"latitude": 47.0, "longitude": 15.4}
                      },
                      "route": {
                        "routeSource": "OPENROUTESERVICE",
                        "routeProfile": "cycling-regular",
                        "startCoordinate": {"latitude": 48.2, "longitude": 16.3},
                        "endCoordinate": {"latitude": 47.0, "longitude": 15.4},
                        "midpointCoordinate": {"latitude": 47.6, "longitude": 15.9},
                        "routeFetchedAt": "2026-06-21T10:00:00Z"
                      },
                      "plannedDistanceM": 12000,
                      "estimatedDurationS": 3600,
                      "logs": []
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/tours/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importedTours").value(2))
                .andExpect(jsonPath("$.importedLogs").value(3));
    }

    @Test
    void importToursReturns400WhenSchemaVersionIsNull() throws Exception {
        String json = """
                {
                  "schemaVersion": null,
                  "tours": [
                    {
                      "tour": {
                        "name": "Imported Tour",
                        "description": "desc",
                        "startLocation": "A",
                        "endLocation": "B",
                        "transportType": "BIKE",
                        "timezoneId": "Europe/Vienna",
                        "startCoordinate": {"latitude": 48.2, "longitude": 16.3},
                        "endCoordinate": {"latitude": 47.0, "longitude": 15.4}
                      },
                      "route": {
                        "routeSource": "OPENROUTESERVICE",
                        "routeProfile": "cycling-regular",
                        "startCoordinate": {"latitude": 48.2, "longitude": 16.3},
                        "endCoordinate": {"latitude": 47.0, "longitude": 15.4},
                        "midpointCoordinate": {"latitude": 47.6, "longitude": 15.9},
                        "routeFetchedAt": "2026-06-21T10:00:00Z"
                      },
                      "plannedDistanceM": 12000,
                      "estimatedDurationS": 3600,
                      "logs": []
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/tours/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importToursReturns409OnConflictException() throws Exception {
        when(tourImportService.importTours(any(TourImportRequest.class)))
                .thenThrow(new ConflictException("Import conflict"));

        mockMvc.perform(post("/api/tours/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validImportJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Import conflict"));
    }

    @Test
    void importToursReturns502OnUpstreamServiceException() throws Exception {
        when(tourImportService.importTours(any(TourImportRequest.class)))
                .thenThrow(new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Upstream failed"));

        mockMvc.perform(post("/api/tours/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validImportJson()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void suggestToursReturns200WithResults() throws Exception {
        when(tourService.suggestTours("dan", 8))
                .thenReturn(List.of(new TourSuggestionDto(1L, "Danube Tour", "Vienna -> Graz", "Danube")));

        mockMvc.perform(get("/api/tours/suggestions")
                        .param("q", "dan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tourId").value(1))
                .andExpect(jsonPath("$[0].label").value("Danube Tour"));
    }

    @Test
    void suggestLocationsReturns200WithResults() throws Exception {
        when(locationSuggestionService.suggestLocations("wien", 6))
                .thenReturn(List.of(new LocationSuggestionDto(
                        "Wien, Austria", "Vienna", "Austria",
                        new CoordinateDto(new BigDecimal("48.2"), new BigDecimal("16.3")),
                        "Europe/Vienna"
                )));

        mockMvc.perform(get("/api/tours/location-suggestions")
                        .param("q", "wien"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Wien, Austria"));
    }

    private TourDetailDto sampleDetail(Long id) {
        return new TourDetailDto(
                id, 42L, "Test Tour", "Description",
                "Vienna", "Graz", TransportType.BIKE, "Europe/Vienna",
                new BigDecimal("12000"), 3600,
                null, null,
                new ComputedTourAttributesDto(
                        0, 0, PopularityCategory.NEW, "new",
                        0, ChildFriendlinessCategory.UNKNOWN, "unknown"
                ),
                Instant.parse("2026-06-21T10:00:00Z"),
                Instant.parse("2026-06-21T10:00:00Z"),
                0L
        );
    }

    private TourSummaryDto sampleSummary(Long id, String name) {
        return new TourSummaryDto(
                id, 42L, name,
                "Vienna", "Graz", TransportType.BIKE, "Europe/Vienna",
                new BigDecimal("12000"), 3600,
                null,
                new ComputedTourAttributesDto(
                        0, 0, PopularityCategory.NEW, "new",
                        0, ChildFriendlinessCategory.UNKNOWN, "unknown"
                ),
                Instant.parse("2026-06-21T10:00:00Z"),
                Instant.parse("2026-06-21T10:00:00Z")
        );
    }

    private String validCreateTourJson() {
        return """
                {
                  "name": "Test Tour",
                  "description": "A test tour",
                  "startLocation": "Vienna",
                  "endLocation": "Graz",
                  "transportType": "BIKE",
                  "timezoneId": "Europe/Vienna",
                  "startCoordinate": {"latitude": 48.2, "longitude": 16.3},
                  "endCoordinate": {"latitude": 47.0, "longitude": 15.4}
                }
                """;
    }

    private String validUpdateTourJson() {
        return """
                {
                  "name": "Updated Tour",
                  "description": "Updated desc",
                  "startLocation": "Vienna",
                  "endLocation": "Graz",
                  "transportType": "BIKE",
                  "timezoneId": "Europe/Vienna",
                  "startCoordinate": {"latitude": 48.2, "longitude": 16.3},
                  "endCoordinate": {"latitude": 47.0, "longitude": 15.4},
                  "version": 0
                }
                """;
    }

    private String validImportJson() {
        return """
                {
                  "schemaVersion": 1,
                  "tours": [
                    {
                      "tour": {
                        "name": "Imported Tour",
                        "description": "desc",
                        "startLocation": "A",
                        "endLocation": "B",
                        "transportType": "BIKE",
                        "timezoneId": "Europe/Vienna",
                        "startCoordinate": {"latitude": 48.2, "longitude": 16.3},
                        "endCoordinate": {"latitude": 47.0, "longitude": 15.4}
                      },
                      "route": {
                        "routeSource": "OPENROUTESERVICE",
                        "routeProfile": "cycling-regular",
                        "startCoordinate": {"latitude": 48.2, "longitude": 16.3},
                        "endCoordinate": {"latitude": 47.0, "longitude": 15.4},
                        "midpointCoordinate": {"latitude": 47.6, "longitude": 15.9},
                        "routeFetchedAt": "2026-06-21T10:00:00Z"
                      },
                      "plannedDistanceM": 12000,
                      "estimatedDurationS": 3600,
                      "logs": []
                    }
                  ]
                }
                """;
    }
}
