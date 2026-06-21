package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CreateTourRequest;
import org.fhtw.mytourapi.dto.ExportedTourDto;
import org.fhtw.mytourapi.dto.TourExportDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

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
                .extracting((exportedTour) -> exportedTour.tour().id())
                .containsExactly(1L, 2L, 3L, 4L);

        ExportedTourDto firstTour = export.tours().get(0);
        assertThat(firstTour.tour().name()).isEqualTo("Danube Island Evening Ride");
        assertThat(firstTour.tour().coverImage().path()).isEqualTo("intermediate/danube-island.jpg");
        assertThat(firstTour.tour().route().routeSource()).isEqualTo("OPENROUTESERVICE");
        assertThat(firstTour.logs())
                .extracting(TourLogDto::id)
                .containsExactly(101L, 102L, 103L);
        assertThat(firstTour.logs().get(0).weather().provider()).isEqualTo("OPEN_METEO");
        assertThat(firstTour.logs().get(0).weather().weatherDescription()).isEqualTo("clear sky");
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
        assertThat(createdTour.tour().id()).isEqualTo(5L);
        assertThat(createdTour.tour().route().routeSource()).isEqualTo("INTERMEDIATE");
        assertThat(createdTour.tour().route().routeGeometry().path("type").asText()).isEqualTo("FeatureCollection");
        assertThat(createdTour.logs()).isEmpty();
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
