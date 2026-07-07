package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TourSearchIndexTest {

    private final TourSearchIndex searchIndex = new TourSearchIndex();

    @Test
    void matchesReturnsTrueWhenQueryIsBlank() {
        searchIndex.replaceLogs(1L, List.of());
        assertThat(searchIndex.matches(tour(1L), null, null)).isTrue();
        assertThat(searchIndex.matches(tour(1L), "", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L), "   ", null)).isTrue();
    }

    @Test
    void matchesReturnsFalseWhenTourIsNull() {
        assertThat(searchIndex.matches(null, "danube", null)).isFalse();
    }

    @Test
    void matchesByTourName() {
        searchIndex.replaceLogs(1L, List.of());

        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "Evening route"), "danube", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "Evening route"), "dan", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "Evening route"), "alps", null)).isFalse();
    }

    @Test
    void matchesByTourDescription() {
        searchIndex.replaceLogs(1L, List.of());

        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "A scenic evening route"), "scenic", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "A scenic evening route"), "sce", null)).isTrue();
    }

    @Test
    void matchesByLogComment() {
        TourLogDto log = log(1L, 1L, "Beautiful sunset views", (short) 4);
        searchIndex.replaceLogs(1L, List.of(log));

        assertThat(searchIndex.matches(tour(1L), "sunset", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L), "sun", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L), "beautiful", null)).isTrue();
    }

    @Test
    void matchesByWeatherDescription() {
        TourLogDto log = logWithWeather(1L, 1L, "Nice day", "partly cloudy");
        searchIndex.replaceLogs(1L, List.of(log));

        assertThat(searchIndex.matches(tour(1L), "cloudy", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L), "partly", null)).isTrue();
    }

    @Test
    void matchesByComputedAttributesLabel() {
        TourDetailDto tour = tourWithComputedAttributes(1L, "Test Tour", "desc",
                new ComputedTourAttributesDto(3, 60, PopularityCategory.POPULAR, "popular",
                        80, ChildFriendlinessCategory.FAMILY_FRIENDLY, "family friendly"));
        searchIndex.replaceLogs(1L, List.of());

        assertThat(searchIndex.matches(tour, "popular", null)).isTrue();
        assertThat(searchIndex.matches(tour, "family", null)).isTrue();
        assertThat(searchIndex.matches(tour, "child friendliness", null)).isTrue();
    }

    @Test
    void matchesWithMultipleQueryTermsRequiresAllTerms() {
        searchIndex.replaceLogs(1L, List.of());

        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "Evening route"), "danube evening", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "Evening route"), "danube alps", null)).isFalse();
    }

    @Test
    void matchesUsesPrefixMatching() {
        searchIndex.replaceLogs(1L, List.of());

        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "Scenic"), "dan", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "Scenic"), "danube ri", null)).isTrue();
    }

    @Test
    void matchesNormalizesDiacritics() {
        searchIndex.replaceLogs(1L, List.of());

        assertThat(searchIndex.matches(tour(1L, "Café Bächle", "desc"), "cafe", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L, "Café Bächle", "desc"), "bachle", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L, "Café Bächle", "desc"), "café", null)).isTrue();
    }

    @Test
    void matchesIsCaseInsensitive() {
        searchIndex.replaceLogs(1L, List.of());

        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "desc"), "DANUBE", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L, "Danube Ride", "desc"), "DaNuBe", null)).isTrue();
    }

    @Test
    void ratingFilterRejectsToursBelowMinimum() {
        TourLogDto log = log(1L, 1L, "Nice tour", (short) 3);
        searchIndex.replaceLogs(1L, List.of(log));

        assertThat(searchIndex.matches(tour(1L), "nice", (short) 3)).isTrue();
        assertThat(searchIndex.matches(tour(1L), "nice", (short) 4)).isFalse();
    }

    @Test
    void ratingFilterAllowsNullMinimum() {
        TourLogDto log = log(1L, 1L, "Nice tour", (short) 2);
        searchIndex.replaceLogs(1L, List.of(log));

        assertThat(searchIndex.matches(tour(1L), "nice", null)).isTrue();
    }

    @Test
    void ratingFilterUsesMaxRatingAcrossLogs() {
        TourLogDto log1 = log(1L, 1L, "First", (short) 2);
        TourLogDto log2 = log(2L, 1L, "Second", (short) 5);
        searchIndex.replaceLogs(1L, List.of(log1, log2));

        assertThat(searchIndex.matches(tour(1L), "first", (short) 5)).isTrue();
        assertThat(searchIndex.matches(tour(1L), "second", (short) 5)).isTrue();
    }

    @Test
    void removeTourClearsLogDocument() {
        TourLogDto log = log(1L, 1L, "Unique comment", (short) 4);
        searchIndex.replaceLogs(1L, List.of(log));
        assertThat(searchIndex.matches(tour(1L), "unique", null)).isTrue();

        searchIndex.removeTour(1L);
        assertThat(searchIndex.matches(tour(1L), "unique", null)).isFalse();
    }

    @Test
    void replaceLogsWithEmptyListClearsLogDocument() {
        TourLogDto log = log(1L, 1L, "Unique comment", (short) 4);
        searchIndex.replaceLogs(1L, List.of(log));
        assertThat(searchIndex.matches(tour(1L), "unique", null)).isTrue();

        searchIndex.replaceLogs(1L, List.of());
        assertThat(searchIndex.matches(tour(1L), "unique", null)).isFalse();
    }

    @Test
    void replaceLogsWithNullClearsLogDocument() {
        TourLogDto log = log(1L, 1L, "Unique comment", (short) 4);
        searchIndex.replaceLogs(1L, List.of(log));
        assertThat(searchIndex.matches(tour(1L), "unique", null)).isTrue();

        searchIndex.replaceLogs(1L, null);
        assertThat(searchIndex.matches(tour(1L), "unique", null)).isFalse();
    }

    @Test
    void matchesByTransportType() {
        searchIndex.replaceLogs(1L, List.of());

        assertThat(searchIndex.matches(tour(1L), "bike", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L), "hike", null)).isFalse();
    }

    @Test
    void matchesByStartAndEndLocation() {
        searchIndex.replaceLogs(1L, List.of());

        assertThat(searchIndex.matches(tour(1L), "praterstern", null)).isTrue();
        assertThat(searchIndex.matches(tour(1L), "donauinsel", null)).isTrue();
    }

    private static TourDetailDto tour(Long id) {
        return tour(id, "Danube Ride", "Evening route");
    }

    private static TourDetailDto tour(Long id, String name, String description) {
        return new TourDetailDto(
                id,
                1L,
                name,
                description,
                "Wien Praterstern",
                "Donauinsel Nord",
                TransportType.BIKE,
                "Europe/Vienna",
                new BigDecimal("18200"),
                4200,
                null,
                null,
                null,
                Instant.parse("2026-04-02T08:15:00Z"),
                Instant.parse("2026-05-10T17:30:00Z"),
                1L
        );
    }

    private static TourDetailDto tourWithComputedAttributes(
            Long id,
            String name,
            String description,
            ComputedTourAttributesDto computedAttributes
    ) {
        return new TourDetailDto(
                id,
                1L,
                name,
                description,
                "Wien Praterstern",
                "Donauinsel Nord",
                TransportType.BIKE,
                "Europe/Vienna",
                new BigDecimal("18200"),
                4200,
                null,
                null,
                computedAttributes,
                Instant.parse("2026-04-02T08:15:00Z"),
                Instant.parse("2026-05-10T17:30:00Z"),
                1L
        );
    }

    private static TourLogDto log(Long id, Long tourId, String comment, Short rating) {
        return new TourLogDto(
                id,
                tourId,
                Instant.parse("2026-06-21T10:00:00Z"),
                comment,
                (short) 3,
                new BigDecimal("5000"),
                3600,
                rating,
                null,
                Instant.parse("2026-06-21T10:05:00Z"),
                Instant.parse("2026-06-21T10:05:00Z"),
                1L
        );
    }

    private static TourLogDto logWithWeather(Long id, Long tourId, String comment, String weatherDescription) {
        return new TourLogDto(
                id,
                tourId,
                Instant.parse("2026-06-21T10:00:00Z"),
                comment,
                (short) 3,
                new BigDecimal("5000"),
                3600,
                (short) 4,
                new org.fhtw.mytourapi.dto.TourLogWeatherDto(
                        id,
                        "OPEN_METEO",
                        "historical-hourly",
                        new org.fhtw.mytourapi.dto.CoordinateDto(new BigDecimal("48.25"), new BigDecimal("16.38")),
                        Instant.parse("2026-06-21T10:00:00Z"),
                        new BigDecimal("18.6"),
                        new BigDecimal("52"),
                        BigDecimal.ZERO,
                        2,
                        weatherDescription,
                        new BigDecimal("11.2"),
                        Instant.parse("2026-06-21T10:00:00Z")
                ),
                Instant.parse("2026-06-21T10:05:00Z"),
                Instant.parse("2026-06-21T10:05:00Z"),
                1L
        );
    }
}
