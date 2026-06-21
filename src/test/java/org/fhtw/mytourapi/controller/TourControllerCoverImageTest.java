package org.fhtw.mytourapi.controller;

import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.exception.ApiErrorResponseFactory;
import org.fhtw.mytourapi.exception.ApiExceptionHandler;
import org.fhtw.mytourapi.service.CoverImageStorageService;
import org.fhtw.mytourapi.service.IntermediateTourSearchIndex;
import org.fhtw.mytourapi.service.IntermediateTourService;
import org.fhtw.mytourapi.service.RouteCalculationService;
import org.fhtw.mytourapi.service.TourAttributeCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TourControllerCoverImageTest {

    @TempDir
    private Path tempDirectory;

    private IntermediateTourService tourService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        tourService = new IntermediateTourService(
                routeCalculationService(),
                coverImageStorageService(),
                new TourAttributeCalculator(),
                new IntermediateTourSearchIndex()
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TourController(tourService))
                .setControllerAdvice(new ApiExceptionHandler(new ApiErrorResponseFactory()))
                .build();
    }

    @Test
    void uploadCoverImageReturnsStoredMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                "image-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(putMultipart("/api/tours/{tourId}/cover-image", 2L).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path", startsWith("covers/")))
                .andExpect(jsonPath("$.originalFilename").value("cover.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(13));

        assertThat(tourService.getTour(2L).orElseThrow().coverImage()).isNotNull();
    }

    @Test
    void uploadCoverImageForMissingTourReturnsNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                "image-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(putMultipart("/api/tours/{tourId}/cover-image", 999L).file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Tour not found"));
    }

    @Test
    void deleteCoverImageReturnsNoContentAndClearsMetadata() throws Exception {
        tourService.uploadCoverImage(
                2L,
                new MockMultipartFile(
                        "file",
                        "cover.png",
                        "image/png",
                        "image-content".getBytes(StandardCharsets.UTF_8)
                )
        );

        mockMvc.perform(delete("/api/tours/{tourId}/cover-image", 2L))
                .andExpect(status().isNoContent());

        assertThat(tourService.getTour(2L).orElseThrow().coverImage()).isNull();
    }

    private MockMultipartHttpServletRequestBuilder putMultipart(String urlTemplate, Object... uriVariables) {
        return (MockMultipartHttpServletRequestBuilder) multipart(urlTemplate, uriVariables)
                .with((request) -> {
                    request.setMethod("PUT");
                    return request;
                });
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
