package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
import org.fhtw.mytourapi.dto.CreateTourLogRequest;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.UpdateTourLogRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IntermediateTourLogServiceComputedAttributesTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void createAndDeleteLogRefreshesComputedTourAttributes() {
        IntermediateTourSearchIndex tourSearchIndex = new IntermediateTourSearchIndex();
        IntermediateTourService tourService = tourService(tourSearchIndex);
        IntermediateTourLogService logService = new IntermediateTourLogService(tourService, tourSearchIndex);

        assertThat(tourService.getTour(4L).orElseThrow().computedAttributes().logCount()).isZero();

        logService.createLog(
                4L,
                new CreateTourLogRequest(
                        Instant.parse("2026-06-21T10:00:00Z"),
                        "Easy family test",
                        (short) 1,
                        new BigDecimal("2000"),
                        1200,
                        (short) 5
                )
        );

        ComputedTourAttributesDto afterCreate = tourService.getTour(4L).orElseThrow().computedAttributes();
        assertThat(afterCreate.logCount()).isEqualTo(1);
        assertThat(afterCreate.popularityCategory()).isEqualTo(PopularityCategory.RARELY_USED);
        assertThat(afterCreate.childFriendlinessCategory()).isEqualTo(ChildFriendlinessCategory.FAMILY_FRIENDLY);

        assertThat(logService.deleteLog(4L, 401L)).isTrue();

        ComputedTourAttributesDto afterDelete = tourService.getTour(4L).orElseThrow().computedAttributes();
        assertThat(afterDelete.logCount()).isZero();
        assertThat(afterDelete.popularityCategory()).isEqualTo(PopularityCategory.NEW);
        assertThat(afterDelete.childFriendlinessCategory()).isEqualTo(ChildFriendlinessCategory.UNKNOWN);
    }

    @Test
    void updateLogRefreshesComputedTourAttributes() {
        IntermediateTourSearchIndex tourSearchIndex = new IntermediateTourSearchIndex();
        IntermediateTourService tourService = tourService(tourSearchIndex);
        IntermediateTourLogService logService = new IntermediateTourLogService(tourService, tourSearchIndex);

        assertThat(tourService.getTour(2L).orElseThrow().computedAttributes().childFriendlinessCategory())
                .isEqualTo(ChildFriendlinessCategory.CHALLENGING_ROUTE);

        logService.updateLog(
                2L,
                201L,
                new UpdateTourLogRequest(
                        Instant.parse("2026-06-21T10:00:00Z"),
                        "Reclassified as easier",
                        (short) 1,
                        new BigDecimal("1200"),
                        900,
                        (short) 5,
                        1L
                )
        );

        ComputedTourAttributesDto afterUpdate = tourService.getTour(2L).orElseThrow().computedAttributes();
        assertThat(afterUpdate.logCount()).isEqualTo(1);
        assertThat(afterUpdate.childFriendlinessCategory()).isEqualTo(ChildFriendlinessCategory.FAMILY_FRIENDLY);
        assertThat(afterUpdate.childFriendlinessLabel()).isEqualTo("family friendly");
    }

    private IntermediateTourService tourService(IntermediateTourSearchIndex tourSearchIndex) {
        return new IntermediateTourService(
                routeCalculationService(),
                coverImageStorageService(),
                new TourAttributeCalculator(),
                tourSearchIndex
        );
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
