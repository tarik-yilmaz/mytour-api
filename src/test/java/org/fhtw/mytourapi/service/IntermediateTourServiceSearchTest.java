package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.CreateTourLogRequest;
import org.fhtw.mytourapi.dto.TourSearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntermediateTourServiceSearchTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void searchMatchesTourAndLogTermsAcrossOneDocument() {
        SearchFixture fixture = searchFixture();

        assertThat(tourIds(fixture.tourService().searchTours("beginners", null, null, null, null)))
                .containsExactly(1L);
        assertThat(tourIds(fixture.tourService().searchTours("sunrise children", null, null, null, null)))
                .containsExactly(2L);
    }

    @Test
    void searchMatchesComputedLabelsAndStructuredComputedFilters() {
        SearchFixture fixture = searchFixture();

        assertThat(tourIds(fixture.tourService().searchTours("very popular", null, null, null, null)))
                .containsExactly(3L);
        assertThat(tourIds(fixture.tourService().searchTours(
                "challenging route",
                null,
                null,
                ChildFriendlinessCategory.CHALLENGING_ROUTE,
                null
        ))).containsExactly(2L);
    }

    @Test
    void searchAppliesRatingMinFromTourLogs() {
        SearchFixture fixture = searchFixture();

        assertThat(tourIds(fixture.tourService().searchTours(null, null, null, null, (short) 5)))
                .containsExactly(1L, 2L, 3L);
        assertThat(tourIds(fixture.tourService().searchTours("vacation", null, null, null, (short) 5)))
                .isEmpty();
        assertThat(tourIds(fixture.tourService().searchTours("best time", null, null, null, (short) 5)))
                .containsExactly(3L);
    }

    @Test
    void searchIndexTracksCreatedAndDeletedLogs() {
        SearchFixture fixture = searchFixture();

        fixture.logService().createLog(
                4L,
                new CreateTourLogRequest(
                        Instant.parse("2026-06-21T11:00:00Z"),
                        "Museum stroller break near the lake.",
                        (short) 1,
                        new BigDecimal("1200"),
                        900,
                        (short) 5
                )
        );

        assertThat(tourIds(fixture.tourService().searchTours("stroller", null, null, null, null)))
                .containsExactly(4L);

        assertThat(fixture.logService().deleteLog(4L, 401L)).isTrue();

        assertThat(tourIds(fixture.tourService().searchTours("stroller", null, null, null, null)))
                .isEmpty();
    }

    private SearchFixture searchFixture() {
        IntermediateTourSearchIndex tourSearchIndex = new IntermediateTourSearchIndex();
        IntermediateTourService tourService = new IntermediateTourService(
                routeCalculationService(),
                coverImageStorageService(),
                new TourAttributeCalculator(),
                tourSearchIndex
        );
        IntermediateTourLogService logService = new IntermediateTourLogService(tourService, tourSearchIndex);

        return new SearchFixture(tourService, logService);
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

    private static List<Long> tourIds(TourSearchResponse response) {
        return response.tours().stream()
                .map((tour) -> tour.id())
                .toList();
    }

    private record SearchFixture(
            IntermediateTourService tourService,
            IntermediateTourLogService logService
    ) {
    }
}
