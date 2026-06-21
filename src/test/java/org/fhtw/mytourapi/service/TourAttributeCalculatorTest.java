package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class TourAttributeCalculatorTest {

    private final TourAttributeCalculator calculator = new TourAttributeCalculator();

    @Test
    void calculateReturnsNewAndUnknownWhenNoLogsExist() {
        ComputedTourAttributesDto result = calculator.calculate(List.of());

        assertThat(result.logCount()).isZero();
        assertThat(result.popularityScore()).isZero();
        assertThat(result.popularityCategory()).isEqualTo(PopularityCategory.NEW);
        assertThat(result.popularityLabel()).isEqualTo("new");
        assertThat(result.childFriendlinessScore()).isZero();
        assertThat(result.childFriendlinessCategory()).isEqualTo(ChildFriendlinessCategory.UNKNOWN);
        assertThat(result.childFriendlinessLabel()).isEqualTo("unknown");
    }

    @Test
    void calculatePopularityFromLogCount() {
        ComputedTourAttributesDto result = calculator.calculate(logs(5, (short) 2, "3000", 1800));

        assertThat(result.logCount()).isEqualTo(5);
        assertThat(result.popularityScore()).isEqualTo(100);
        assertThat(result.popularityCategory()).isEqualTo(PopularityCategory.VERY_POPULAR);
        assertThat(result.popularityLabel()).isEqualTo("very popular");
    }

    @Test
    void calculateFamilyFriendlyForEasyShortLogs() {
        ComputedTourAttributesDto result = calculator.calculate(logs(2, (short) 1, "2500", 1800));

        assertThat(result.childFriendlinessScore()).isGreaterThanOrEqualTo(75);
        assertThat(result.childFriendlinessCategory()).isEqualTo(ChildFriendlinessCategory.FAMILY_FRIENDLY);
        assertThat(result.childFriendlinessLabel()).isEqualTo("family friendly");
    }

    @Test
    void calculateModerateFamilySuitabilityForAverageLogs() {
        ComputedTourAttributesDto result = calculator.calculate(logs(2, (short) 3, "5000", 3600));

        assertThat(result.childFriendlinessScore()).isBetween(50, 74);
        assertThat(result.childFriendlinessCategory())
                .isEqualTo(ChildFriendlinessCategory.MODERATE_FAMILY_SUITABILITY);
        assertThat(result.childFriendlinessLabel()).isEqualTo("moderate family suitability");
    }

    @Test
    void calculateChallengingRouteForHarderLogs() {
        ComputedTourAttributesDto result = calculator.calculate(logs(1, (short) 4, "8000", 7200));

        assertThat(result.childFriendlinessScore()).isBetween(25, 49);
        assertThat(result.childFriendlinessCategory()).isEqualTo(ChildFriendlinessCategory.CHALLENGING_ROUTE);
        assertThat(result.childFriendlinessLabel()).isEqualTo("challenging route");
    }

    @Test
    void calculateAdultOrientedForVeryHardLongLogs() {
        ComputedTourAttributesDto result = calculator.calculate(logs(1, (short) 5, "25000", 14400));

        assertThat(result.childFriendlinessScore()).isLessThan(25);
        assertThat(result.childFriendlinessCategory()).isEqualTo(ChildFriendlinessCategory.ADULT_ORIENTED);
        assertThat(result.childFriendlinessLabel()).isEqualTo("adult oriented");
    }

    private static List<TourLogDto> logs(
            int count,
            Short difficulty,
            String totalDistanceM,
            Integer totalTimeS
    ) {
        return IntStream.range(0, count)
                .mapToObj((index) -> log((long) index + 1L, difficulty, totalDistanceM, totalTimeS))
                .toList();
    }

    private static TourLogDto log(Long id, Short difficulty, String totalDistanceM, Integer totalTimeS) {
        Instant performedAt = Instant.parse("2026-06-21T10:00:00Z").plusSeconds(id * 3600);
        return new TourLogDto(
                id,
                1L,
                performedAt,
                "test log",
                difficulty,
                new BigDecimal(totalDistanceM),
                totalTimeS,
                (short) 4,
                null,
                performedAt,
                performedAt,
                1L
        );
    }
}
