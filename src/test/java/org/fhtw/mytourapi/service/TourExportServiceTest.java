package org.fhtw.mytourapi.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CreateTourRequest;
import org.fhtw.mytourapi.dto.ExportedTourDto;
import org.fhtw.mytourapi.dto.TourExportDto;
import org.fhtw.mytourapi.dto.TourImportRequest;
import org.fhtw.mytourapi.dto.TransportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TourExportServiceTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void exportToursIncludesSchemaAndStableTourLogWeatherSnapshots() {
        ExportFixture fixture = exportFixture();

        TourExportDto export = fixture.exportService().exportTours();

        assertThat(export.schemaVersion()).isEqualTo(1);
        assertThat(export.exportedAt()).isNotNull();
        assertThat(export.tours())
                .extracting((exportedTour) -> exportedTour.tour().name())
                .containsExactly(
                        "Danube Island Evening Ride",
                        "Kahlenberg Sunrise Hike",
                        "Ringstrasse Lunch Run",
                        "Salzkammergut Weekend"
                );

        ExportedTourDto firstTour = export.tours().get(0);
        assertThat(firstTour.tour().name()).isEqualTo("Danube Island Evening Ride");
        assertThat(firstTour.tour().startCoordinate()).isEqualTo(firstTour.route().startCoordinate());
        assertThat(firstTour.coverImage().path()).isEqualTo("intermediate/danube-island.jpg");
        assertThat(firstTour.route().routeSource()).isEqualTo("OPENROUTESERVICE");
        assertThat(firstTour.logs())
                .extracting((exportedLog) -> exportedLog.log().performedAt())
                .containsExactly(
                        Instant.parse("2026-05-10T17:45:00Z"),
                        Instant.parse("2026-05-12T18:10:00Z"),
                        Instant.parse("2026-05-15T17:30:00Z")
                );
        assertThat(firstTour.logs().get(0).weather().provider()).isEqualTo("OPEN_METEO");
        assertThat(firstTour.logs().get(0).weather().weatherDescription()).isEqualTo("clear sky");
        assertThat(firstTour.logs().get(0).weather().fetchedAt()).isNotNull();
    }

    @Test
    void exportToursIncludesCurrentRouteGeometryForNewTours() {
        ExportFixture fixture = exportFixture();

        fixture.tourService().createTour(new CreateTourRequest(
                "Export Geometry Test",
                "Route geometry should be part of the exported tour snapshot.",
                "Schottenring",
                "Karlsplatz",
                TransportType.BIKE,
                "Europe/Vienna",
                coordinate("48.2167", "16.3719"),
                coordinate("48.2007", "16.3695")
        ));

        TourExportDto export = fixture.exportService().exportTours();

        ExportedTourDto createdTour = export.tours().get(4);
        assertThat(createdTour.tour().name()).isEqualTo("Export Geometry Test");
        assertThat(createdTour.route().routeSource()).isEqualTo("INTERMEDIATE");
        assertThat(createdTour.route().routeGeometry().path("type").asText()).isEqualTo("FeatureCollection");
        assertThat(createdTour.logs()).isEmpty();
    }

    @Test
    void exportJsonCanBeReadAsImportRequestShape() throws Exception {
        ExportFixture fixture = exportFixture();
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String exportJson = objectMapper.writeValueAsString(fixture.exportService().exportTours());
        TourImportRequest importRequest = objectMapper.readValue(exportJson, TourImportRequest.class);

        assertThat(importRequest.schemaVersion()).isEqualTo(1);
        assertThat(importRequest.tours()).hasSize(4);
        assertThat(importRequest.tours().get(0).tour().name()).isEqualTo("Danube Island Evening Ride");
        assertThat(importRequest.tours().get(0).route().routeSource()).isEqualTo("OPENROUTESERVICE");
        assertThat(importRequest.tours().get(0).logs().get(0).weather().provider()).isEqualTo("OPEN_METEO");
        assertThat(importRequest.tours().get(3).logs()).isEmpty();
    }

    private ExportFixture exportFixture() {
        IntermediateTourSearchIndex tourSearchIndex = new IntermediateTourSearchIndex();
        IntermediateTourService tourService = new IntermediateTourService(
                routeCalculationService(),
                coverImageStorageService(),
                new TourAttributeCalculator(),
                tourSearchIndex
        );
        IntermediateTourLogService tourLogService = new IntermediateTourLogService(tourService, tourSearchIndex);
        TourExportService exportService = new TourExportService(tourService, tourLogService);

        return new ExportFixture(tourService, exportService);
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

    private static CoordinateDto coordinate(String latitude, String longitude) {
        return new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude));
    }

    private record ExportFixture(
            IntermediateTourService tourService,
            TourExportService exportService
    ) {
    }
}
