package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TourSearchResponse;
import org.fhtw.mytourapi.dto.TourSummaryDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IntermediateTourService {

    private final Map<Long, TourDetailDto> toursById = new ConcurrentHashMap<>(Map.of(
            1L, new TourDetailDto(
                    1L,
                    1L,
                    "Danube Island Evening Ride",
                    "Easy after-work cycling route along the Danube with wide paths and a calm finish near the water.",
                    "Wien Praterstern",
                    "Donauinsel Nord",
                    TransportType.BIKE,
                    "Europe/Vienna",
                    meters("18200"),
                    4200,
                    new CoverImageDto("intermediate/danube-island.jpg", "danube-island.jpg", "image/jpeg", 264000L),
                    new TourRouteDto(
                            "OPENROUTESERVICE",
                            "cycling-regular",
                            coordinate("48.2189", "16.3927"),
                            coordinate("48.2872", "16.3674"),
                            coordinate("48.2530", "16.3801"),
                            null,
                            Instant.parse("2026-05-10T17:30:00Z")
                    ),
                    new ComputedTourAttributesDto(
                            3,
                            68,
                            PopularityCategory.POPULAR,
                            "popular",
                            88,
                            ChildFriendlinessCategory.FAMILY_FRIENDLY,
                            "family friendly"
                    ),
                    Instant.parse("2026-04-02T08:15:00Z"),
                    Instant.parse("2026-05-10T17:30:00Z"),
                    1L
            ),
            2L, new TourDetailDto(
                    2L,
                    1L,
                    "Kahlenberg Sunrise Hike",
                    "A compact morning hike from Nussdorf up to Kahlenberg with a steady climb and a clear city view.",
                    "Nussdorf",
                    "Kahlenberg",
                    TransportType.HIKE,
                    "Europe/Vienna",
                    meters("7600"),
                    8100,
                    null,
                    new TourRouteDto(
                            "OPENROUTESERVICE",
                            "foot-hiking",
                            coordinate("48.2601", "16.3688"),
                            coordinate("48.2767", "16.3339"),
                            coordinate("48.2684", "16.3512"),
                            null,
                            Instant.parse("2026-05-03T06:25:00Z")
                    ),
                    new ComputedTourAttributesDto(
                            1,
                            24,
                            PopularityCategory.RARELY_USED,
                            "rarely used",
                            36,
                            ChildFriendlinessCategory.CHALLENGING_ROUTE,
                            "challenging route"
                    ),
                    Instant.parse("2026-03-19T06:00:00Z"),
                    Instant.parse("2026-05-03T06:25:00Z"),
                    1L
            ),
            3L, new TourDetailDto(
                    3L,
                    1L,
                    "Ringstrasse Lunch Run",
                    "Short urban running loop for lunch breaks, with predictable streets and several easy exit points.",
                    "Stadtpark",
                    "Rathausplatz",
                    TransportType.RUNNING,
                    "Europe/Vienna",
                    meters("5100"),
                    1800,
                    null,
                    new TourRouteDto(
                            "OPENROUTESERVICE",
                            "foot-walking",
                            coordinate("48.2042", "16.3802"),
                            coordinate("48.2109", "16.3576"),
                            coordinate("48.2075", "16.3689"),
                            null,
                            Instant.parse("2026-05-15T12:20:00Z")
                    ),
                    new ComputedTourAttributesDto(
                            5,
                            92,
                            PopularityCategory.VERY_POPULAR,
                            "very popular",
                            18,
                            ChildFriendlinessCategory.ADULT_ORIENTED,
                            "adult oriented"
                    ),
                    Instant.parse("2026-02-11T11:45:00Z"),
                    Instant.parse("2026-05-15T12:20:00Z"),
                    1L
            ),
            4L, new TourDetailDto(
                    4L,
                    1L,
                    "Salzkammergut Weekend",
                    "Vacation tour draft from Bad Ischl to Hallstatt for a relaxed weekend route with photo stops.",
                    "Bad Ischl",
                    "Hallstatt",
                    TransportType.VACATION,
                    "Europe/Vienna",
                    meters("22400"),
                    14400,
                    null,
                    new TourRouteDto(
                            "OPENROUTESERVICE",
                            "driving-car",
                            coordinate("47.7111", "13.6236"),
                            coordinate("47.5622", "13.6493"),
                            coordinate("47.6367", "13.6365"),
                            null,
                            Instant.parse("2026-05-01T09:00:00Z")
                    ),
                    new ComputedTourAttributesDto(
                            0,
                            0,
                            PopularityCategory.NEW,
                            "new",
                            0,
                            ChildFriendlinessCategory.UNKNOWN,
                            "unknown"
                    ),
                    Instant.parse("2026-05-01T09:00:00Z"),
                    Instant.parse("2026-05-01T09:00:00Z"),
                    1L
            )
    ));

    public TourSearchResponse searchTours(
            String query,
            TransportType transportType,
            PopularityCategory popularity,
            ChildFriendlinessCategory childFriendliness
    ) {
        List<TourSummaryDto> tours = toursById.values().stream()
                .filter((tour) -> matchesQuery(tour, query))
                .filter((tour) -> transportType == null || tour.transportType() == transportType)
                .filter((tour) -> popularity == null || tour.computedAttributes().popularityCategory() == popularity)
                .filter((tour) -> childFriendliness == null
                        || tour.computedAttributes().childFriendlinessCategory() == childFriendliness)
                .map(this::toSummary)
                .sorted(Comparator.comparing(TourSummaryDto::id))
                .toList();

        return new TourSearchResponse(tours, tours.size());
    }

    public Optional<TourDetailDto> getTour(Long tourId) {
        return Optional.ofNullable(toursById.get(tourId));
    }

    public boolean deleteTour(Long tourId) {
        return toursById.remove(tourId) != null;
    }

    private boolean matchesQuery(TourDetailDto tour, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase();
        String searchableText = String.join(" ",
                safeText(tour.name()),
                safeText(tour.description()),
                safeText(tour.startLocation()),
                safeText(tour.endLocation()),
                tour.transportType().name(),
                safeText(tour.computedAttributes().popularityLabel()),
                safeText(tour.computedAttributes().childFriendlinessLabel())
        ).toLowerCase();

        return searchableText.contains(normalizedQuery);
    }

    private TourSummaryDto toSummary(TourDetailDto tour) {
        return new TourSummaryDto(
                tour.id(),
                tour.userId(),
                tour.name(),
                tour.startLocation(),
                tour.endLocation(),
                tour.transportType(),
                tour.timezoneId(),
                tour.plannedDistanceM(),
                tour.estimatedDurationS(),
                tour.coverImage(),
                tour.computedAttributes(),
                tour.createdAt(),
                tour.updatedAt()
        );
    }

    private static CoordinateDto coordinate(String latitude, String longitude) {
        return new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude));
    }

    private static BigDecimal meters(String meters) {
        return new BigDecimal(meters);
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }
}
