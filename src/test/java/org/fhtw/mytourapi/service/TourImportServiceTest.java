package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.dto.CreateTourLogRequest;
import org.fhtw.mytourapi.dto.CreateTourRequest;
import org.fhtw.mytourapi.dto.ImportResultDto;
import org.fhtw.mytourapi.dto.ImportedTourDto;
import org.fhtw.mytourapi.dto.ImportedTourLogDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourImportRequest;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.fhtw.mytourapi.exception.ImportValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourImportServiceTest {

    @Test
    void importToursRejectsNullRequest() {
        assertThatThrownBy(() -> importService().importTours(null))
                .isInstanceOf(ImportValidationException.class)
                .satisfies(hasErrorContaining("request: must not be null"));
    }

    @Test
    void importToursRejectsUnsupportedSchemaVersion() {
        TourImportRequest request = new TourImportRequest(99, List.of(validImportedTour()));
        assertThatThrownBy(() -> importService().importTours(request))
                .isInstanceOf(ImportValidationException.class)
                .satisfies(hasErrorContaining("schemaVersion: unsupported version 99"));
    }

    @Test
    void importToursRejectsRouteCoordinatesMismatch() {
        CoordinateDto tourStart = coordinate("48.2082", "16.3738");
        CoordinateDto routeStart = coordinate("48.9999", "16.9999");
        CreateTourRequest tourRequest = new CreateTourRequest(
                "Test Tour", "desc", "Start", "End",
                TransportType.BIKE, "Europe/Vienna", tourStart, coordinate("48.2500", "16.4000")
        );
        TourRouteDto route = new TourRouteDto(
                "OPENROUTESERVICE", "cycling-regular", routeStart, coordinate("48.2500", "16.4000"),
                coordinate("48.2300", "16.3900"), null, Instant.parse("2026-06-21T10:00:00Z")
        );
        ImportedTourDto importedTour = new ImportedTourDto(
                tourRequest, route, null, new BigDecimal("5000"), 3600, List.of()
        );
        assertThatThrownBy(() -> importService().importTours(new TourImportRequest(1, List.of(importedTour))))
                .isInstanceOf(ImportValidationException.class)
                .satisfies(hasErrorContaining("route.startCoordinate: must match tour.startCoordinate"));
    }

    @Test
    void importToursRejectsUnsafeCoverImagePath() {
        CoverImageDto coverImage = new CoverImageDto("../escape.jpg", "escape.jpg", "image/jpeg", 100L);
        ImportedTourDto importedTour = new ImportedTourDto(
                validCreateTourRequest(), validRoute(), coverImage, new BigDecimal("5000"), 3600, List.of()
        );
        assertThatThrownBy(() -> importService().importTours(new TourImportRequest(1, List.of(importedTour))))
                .isInstanceOf(ImportValidationException.class)
                .satisfies(hasErrorContaining("coverImage.path: must be a safe relative path"));
    }

    @Test
    void importToursRejectsAbsoluteCoverImagePath() {
        CoverImageDto coverImage = new CoverImageDto("C:\\Windows\\system32", "system32", "image/jpeg", 100L);
        ImportedTourDto importedTour = new ImportedTourDto(
                validCreateTourRequest(), validRoute(), coverImage, new BigDecimal("5000"), 3600, List.of()
        );
        assertThatThrownBy(() -> importService().importTours(new TourImportRequest(1, List.of(importedTour))))
                .isInstanceOf(ImportValidationException.class)
                .satisfies(hasErrorContaining("coverImage.path: must be a safe relative path"));
    }

    @Test
    void importToursRejectsMultipleErrorsAtOnce() {
        ImportedTourDto importedTour = new ImportedTourDto(
                null, null, null, null, null, null
        );
        assertThatThrownBy(() -> importService().importTours(new TourImportRequest(null, List.of(importedTour))))
                .isInstanceOf(ImportValidationException.class)
                .satisfies((exception) -> {
                    List<String> errors = ((ImportValidationException) exception).validationErrors();
                    assertThat(errors).hasSizeGreaterThanOrEqualTo(5);
                    assertThat(errors).anyMatch((error) -> error.contains("schemaVersion: must not be null"));
                    assertThat(errors).anyMatch((error) -> error.contains("tours[0].tour: must not be null"));
                    assertThat(errors).anyMatch((error) -> error.contains("tours[0].route: must not be null"));
                    assertThat(errors).anyMatch((error) -> error.contains("tours[0].plannedDistanceM: must not be null"));
                    assertThat(errors).anyMatch((error) -> error.contains("tours[0].estimatedDurationS: must not be null"));
                    assertThat(errors).anyMatch((error) -> error.contains("tours[0].logs: must not be null"));
                });
    }

    @Test
    void importToursSucceedsWithValidRequest() {
        ImportResultDto result = importService().importTours(
                new TourImportRequest(1, List.of(validImportedTour()))
        );

        assertThat(result.importedTours()).isEqualTo(1);
        assertThat(result.importedLogs()).isEqualTo(1);
        assertThat(result.createdTourIds()).containsExactly(1L);
    }

    private static TourImportService importService() {
        TourService tourService = new TourService(
                null, null, null, null, null, null, null, null
        ) {
            @Override
            public TourDetailDto importTour(ImportedTourDto importedTour) {
                return new TourDetailDto(
                        1L, 1L, "Test", "desc", "Start", "End",
                        TransportType.BIKE, "Europe/Vienna",
                        new BigDecimal("5000"), 3600, null, null, null,
                        Instant.parse("2026-06-21T10:00:00Z"),
                        Instant.parse("2026-06-21T10:00:00Z"), 1L
                );
            }
        };
        TourLogService tourLogService = new TourLogService(
                null, null, null, null, null, null
        ) {
            @Override
            public Optional<List<TourLogDto>> importLogs(
                    Long tourId, List<ImportedTourLogDto> importedLogs
            ) {
                return Optional.of(importedLogs.stream()
                        .map((importedLog) -> new TourLogDto(
                                1L, tourId, importedLog.log().performedAt(),
                                importedLog.log().comment(), importedLog.log().difficulty(),
                                importedLog.log().totalDistanceM(), importedLog.log().totalTimeS(),
                                importedLog.log().rating(), null,
                                Instant.parse("2026-06-21T10:00:00Z"),
                                Instant.parse("2026-06-21T10:00:00Z"), 1L
                        ))
                        .toList());
            }
        };
        return new TourImportService(tourService, tourLogService);
    }

    private static ImportedTourDto validImportedTour() {
        return new ImportedTourDto(
                validCreateTourRequest(),
                validRoute(),
                null,
                new BigDecimal("5000"),
                3600,
                List.of(new ImportedTourLogDto(
                        new CreateTourLogRequest(
                                Instant.parse("2026-06-21T10:00:00Z"),
                                "Nice tour",
                                (short) 3,
                                new BigDecimal("5000"),
                                3600,
                                (short) 4
                        ),
                        null
                ))
        );
    }

    private static CreateTourRequest validCreateTourRequest() {
        return new CreateTourRequest(
                "Test Tour", "desc", "Start", "End",
                TransportType.BIKE, "Europe/Vienna",
                coordinate("48.2082", "16.3738"),
                coordinate("48.2500", "16.4000")
        );
    }

    private static TourRouteDto validRoute() {
        return new TourRouteDto(
                "OPENROUTESERVICE", "cycling-regular",
                coordinate("48.2082", "16.3738"),
                coordinate("48.2500", "16.4000"),
                coordinate("48.2300", "16.3900"),
                geoJson(),
                Instant.parse("2026-06-21T10:00:00Z")
        );
    }

    private static CoordinateDto coordinate(String latitude, String longitude) {
        return new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude));
    }

    private static Map<String, Object> geoJson() {
        return Map.of("type", "FeatureCollection", "features", List.of());
    }

    private static Consumer<Throwable> hasErrorContaining(String expected) {
        return (throwable) -> {
            List<String> errors = ((ImportValidationException) throwable).validationErrors();
            assertThat(errors).anyMatch((error) -> error.contains(expected));
        };
    }
}
