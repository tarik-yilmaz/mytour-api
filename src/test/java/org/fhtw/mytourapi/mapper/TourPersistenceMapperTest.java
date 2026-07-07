package org.fhtw.mytourapi.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fhtw.mytourapi.domain.TourEntity;
import org.fhtw.mytourapi.domain.TourLogEntity;
import org.fhtw.mytourapi.domain.TourLogWeatherEntity;
import org.fhtw.mytourapi.domain.TourRouteEntity;
import org.fhtw.mytourapi.domain.UserEntity;
import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.dto.ImportedWeatherSnapshotDto;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TourSummaryDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TourPersistenceMapperTest {

    private TourPersistenceMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mapper = new TourPersistenceMapper();
    }

    @Test
    void toDetailMapsAllFieldsCorrectly() {
        TourEntity tour = fullTour(1L, 42L);

        TourDetailDto dto = mapper.toDetail(tour);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.userId()).isEqualTo(42L);
        assertThat(dto.name()).isEqualTo("Test Tour");
        assertThat(dto.description()).isEqualTo("Description");
        assertThat(dto.startLocation()).isEqualTo("Vienna");
        assertThat(dto.endLocation()).isEqualTo("Graz");
        assertThat(dto.transportType()).isEqualTo(TransportType.BIKE);
        assertThat(dto.timezoneId()).isEqualTo("Europe/Vienna");
        assertThat(dto.plannedDistanceM()).isEqualByComparingTo("12000");
        assertThat(dto.estimatedDurationS()).isEqualTo(3600);
        assertThat(dto.coverImage()).isNotNull();
        assertThat(dto.coverImage().path()).isEqualTo("covers/test.jpg");
        assertThat(dto.route()).isNotNull();
        assertThat(dto.route().routeSource()).isEqualTo("OPENROUTESERVICE");
        assertThat(dto.computedAttributes()).isNotNull();
        assertThat(dto.computedAttributes().logCount()).isEqualTo(3);
        assertThat(dto.computedAttributes().popularityCategory()).isEqualTo(PopularityCategory.POPULAR);
        assertThat(dto.computedAttributes().childFriendlinessCategory())
                .isEqualTo(ChildFriendlinessCategory.FAMILY_FRIENDLY);
        assertThat(dto.version()).isEqualTo(1L);
    }

    @Test
    void toDetailMapsTourWithoutCoverImageOrRoute() {
        TourEntity tour = minimalTour(1L, 42L);

        TourDetailDto dto = mapper.toDetail(tour);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.coverImage()).isNull();
        assertThat(dto.route()).isNull();
        assertThat(dto.computedAttributes()).isNotNull();
        assertThat(dto.computedAttributes().logCount()).isEqualTo(0);
        assertThat(dto.computedAttributes().popularityCategory()).isEqualTo(PopularityCategory.NEW);
    }

    @Test
    void toSummaryMapsAllFieldsCorrectly() {
        TourEntity tour = fullTour(1L, 42L);

        TourSummaryDto dto = mapper.toSummary(tour);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.userId()).isEqualTo(42L);
        assertThat(dto.name()).isEqualTo("Test Tour");
        assertThat(dto.startLocation()).isEqualTo("Vienna");
        assertThat(dto.endLocation()).isEqualTo("Graz");
        assertThat(dto.transportType()).isEqualTo(TransportType.BIKE);
        assertThat(dto.timezoneId()).isEqualTo("Europe/Vienna");
        assertThat(dto.plannedDistanceM()).isEqualByComparingTo("12000");
        assertThat(dto.estimatedDurationS()).isEqualTo(3600);
        assertThat(dto.coverImage()).isNotNull();
        assertThat(dto.computedAttributes()).isNotNull();
        assertThat(dto.computedAttributes().logCount()).isEqualTo(3);
    }

    @Test
    void toLogMapsAllFieldsCorrectly() {
        TourEntity tour = minimalTour(1L, 42L);
        TourLogEntity log = fullLog(10L, tour);
        log.setWeather(fullWeather(log));

        TourLogDto dto = mapper.toLog(log);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.tourId()).isEqualTo(1L);
        assertThat(dto.performedAt()).isEqualTo(Instant.parse("2026-06-21T10:00:00Z"));
        assertThat(dto.comment()).isEqualTo("Great tour");
        assertThat(dto.difficulty()).isEqualTo((short) 3);
        assertThat(dto.totalDistanceM()).isEqualByComparingTo("12000");
        assertThat(dto.totalTimeS()).isEqualTo(3600);
        assertThat(dto.rating()).isEqualTo((short) 4);
        assertThat(dto.weather()).isNotNull();
        assertThat(dto.weather().provider()).isEqualTo("OPEN_METEO");
        assertThat(dto.weather().temperatureC()).isEqualByComparingTo("20.5");
    }

    @Test
    void toLogMapsLogWithoutWeather() {
        TourEntity tour = minimalTour(1L, 42L);
        TourLogEntity log = fullLog(10L, tour);
        log.setWeather(null);

        TourLogDto dto = mapper.toLog(log);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.weather()).isNull();
    }

    @Test
    void toRouteReturnsNullForNullInput() {
        assertThat(mapper.toRoute(null)).isNull();
    }

    @Test
    void toRouteMapsAllFieldsCorrectly() {
        TourRouteEntity route = fullRoute();

        TourRouteDto dto = mapper.toRoute(route);

        assertThat(dto).isNotNull();
        assertThat(dto.routeSource()).isEqualTo("OPENROUTESERVICE");
        assertThat(dto.routeProfile()).isEqualTo("cycling-regular");
        assertThat(dto.startCoordinate().latitude()).isEqualByComparingTo("48.209200");
        assertThat(dto.startCoordinate().longitude()).isEqualByComparingTo("16.404400");
        assertThat(dto.endCoordinate().latitude()).isEqualByComparingTo("48.250000");
        assertThat(dto.midpointCoordinate().latitude()).isEqualByComparingTo("48.229600");
        assertThat(dto.routeGeometry()).containsEntry("type", "LineString");
        assertThat(dto.routeFetchedAt()).isEqualTo(Instant.parse("2026-06-21T10:00:00Z"));
    }

    @Test
    void toWeatherReturnsNullForNullInput() {
        assertThat(mapper.toWeather(null)).isNull();
    }

    @Test
    void toWeatherMapsAllFieldsCorrectly() {
        TourEntity tour = minimalTour(1L, 42L);
        TourLogEntity log = fullLog(10L, tour);
        TourLogWeatherEntity weather = fullWeather(log);

        TourLogWeatherDto dto = mapper.toWeather(weather);

        assertThat(dto).isNotNull();
        assertThat(dto.tourLogId()).isEqualTo(10L);
        assertThat(dto.provider()).isEqualTo("OPEN_METEO");
        assertThat(dto.providerDataset()).isEqualTo("archive");
        assertThat(dto.lookupCoordinate().latitude()).isEqualByComparingTo("48.229600");
        assertThat(dto.weatherObservedAt()).isEqualTo(Instant.parse("2026-06-21T10:00:00Z"));
        assertThat(dto.temperatureC()).isEqualByComparingTo("20.5");
        assertThat(dto.relativeHumidityPercent()).isEqualByComparingTo("60");
        assertThat(dto.precipitationMm()).isEqualByComparingTo("0.0");
        assertThat(dto.weatherCode()).isEqualTo(3);
        assertThat(dto.weatherDescription()).isEqualTo("Overcast");
        assertThat(dto.windSpeedKmh()).isEqualByComparingTo("15.0");
    }

    @Test
    void toCoverImageReturnsNullWhenPathIsNull() {
        TourEntity tour = minimalTour(1L, 42L);

        assertThat(mapper.toCoverImage(tour)).isNull();
    }

    @Test
    void toCoverImageMapsAllFieldsWhenPathIsSet() {
        TourEntity tour = fullTour(1L, 42L);

        CoverImageDto dto = mapper.toCoverImage(tour);

        assertThat(dto).isNotNull();
        assertThat(dto.path()).isEqualTo("covers/test.jpg");
        assertThat(dto.originalFilename()).isEqualTo("test.jpg");
        assertThat(dto.contentType()).isEqualTo("image/jpeg");
        assertThat(dto.sizeBytes()).isEqualTo(1024L);
    }

    @Test
    void toComputedAttributesMapsAllFields() {
        TourEntity tour = fullTour(1L, 42L);

        ComputedTourAttributesDto dto = mapper.toComputedAttributes(tour);

        assertThat(dto.logCount()).isEqualTo(3);
        assertThat(dto.popularityScore()).isEqualTo(2);
        assertThat(dto.popularityCategory()).isEqualTo(PopularityCategory.POPULAR);
        assertThat(dto.popularityLabel()).isEqualTo("popular");
        assertThat(dto.childFriendlinessScore()).isEqualTo(85);
        assertThat(dto.childFriendlinessCategory()).isEqualTo(ChildFriendlinessCategory.FAMILY_FRIENDLY);
        assertThat(dto.childFriendlinessLabel()).isEqualTo("family friendly");
    }

    @Test
    void applyCoverImageSetsFieldsFromDto() {
        TourEntity tour = minimalTour(1L, 42L);
        CoverImageDto coverImage = new CoverImageDto("covers/new.jpg", "new.jpg", "image/jpeg", 2048L);

        mapper.applyCoverImage(tour, coverImage);

        assertThat(tour.getCoverImagePath()).isEqualTo("covers/new.jpg");
        assertThat(tour.getCoverImageOriginalFilename()).isEqualTo("new.jpg");
        assertThat(tour.getCoverImageContentType()).isEqualTo("image/jpeg");
        assertThat(tour.getCoverImageSizeBytes()).isEqualTo(2048L);
    }

    @Test
    void applyCoverImageClearsFieldsWhenNull() {
        TourEntity tour = fullTour(1L, 42L);

        mapper.applyCoverImage(tour, null);

        assertThat(tour.getCoverImagePath()).isNull();
        assertThat(tour.getCoverImageOriginalFilename()).isNull();
        assertThat(tour.getCoverImageContentType()).isNull();
        assertThat(tour.getCoverImageSizeBytes()).isNull();
    }

    @Test
    void applyComputedAttributesSetsAllFields() {
        TourEntity tour = minimalTour(1L, 42L);
        ComputedTourAttributesDto attributes = new ComputedTourAttributesDto(
                5, 4, PopularityCategory.VERY_POPULAR, "very popular",
                30, ChildFriendlinessCategory.ADULT_ORIENTED, "adult oriented"
        );

        mapper.applyComputedAttributes(tour, attributes);

        assertThat(tour.getLogCount()).isEqualTo(5);
        assertThat(tour.getPopularityScore()).isEqualTo(4);
        assertThat(tour.getPopularityCategory()).isEqualTo(org.fhtw.mytourapi.domain.PopularityCategory.VERY_POPULAR);
        assertThat(tour.getPopularityLabel()).isEqualTo("very popular");
        assertThat(tour.getChildFriendlinessScore()).isEqualTo(30);
        assertThat(tour.getChildFriendlinessCategory())
                .isEqualTo(org.fhtw.mytourapi.domain.ChildFriendlinessCategory.ADULT_ORIENTED);
        assertThat(tour.getChildFriendlinessLabel()).isEqualTo("adult oriented");
    }

    @Test
    void applyRouteCreatesNewRouteEntityWhenNoneExists() {
        TourEntity tour = minimalTour(1L, 42L);
        TourRouteDto routeDto = new TourRouteDto(
                "OPENROUTESERVICE", "cycling-regular",
                new CoordinateDto(new BigDecimal("48.2"), new BigDecimal("16.3")),
                new CoordinateDto(new BigDecimal("47.0"), new BigDecimal("15.4")),
                new CoordinateDto(new BigDecimal("47.6"), new BigDecimal("15.9")),
                geoJson(), Instant.parse("2026-06-21T10:00:00Z")
        );

        TourRouteEntity result = mapper.applyRoute(tour, routeDto);

        assertThat(result).isNotNull();
        assertThat(result.getTour()).isSameAs(tour);
        assertThat(tour.getRoute()).isSameAs(result);
        assertThat(result.getRouteSource()).isEqualTo("OPENROUTESERVICE");
        assertThat(result.getRouteProfile()).isEqualTo("cycling-regular");
        assertThat(result.getStartLat()).isEqualByComparingTo("48.2");
        assertThat(result.getStartLon()).isEqualByComparingTo("16.3");
        assertThat(result.getEndLat()).isEqualByComparingTo("47.0");
        assertThat(result.getMidpointLat()).isEqualByComparingTo("47.6");
        assertThat(result.getRouteGeometry().path("type").asText()).isEqualTo("FeatureCollection");
        assertThat(result.getRouteFetchedAt()).isEqualTo(Instant.parse("2026-06-21T10:00:00Z"));
    }

    @Test
    void applyRouteUpdatesExistingRouteEntity() {
        TourEntity tour = fullTour(1L, 42L);
        TourRouteEntity existingRoute = tour.getRoute();
        TourRouteDto routeDto = new TourRouteDto(
                "LOCAL", "foot-hiking",
                new CoordinateDto(new BigDecimal("48.3"), new BigDecimal("16.4")),
                new CoordinateDto(new BigDecimal("47.1"), new BigDecimal("15.5")),
                new CoordinateDto(new BigDecimal("47.7"), new BigDecimal("15.95")),
                geoJson(), Instant.parse("2026-06-22T10:00:00Z")
        );

        TourRouteEntity result = mapper.applyRoute(tour, routeDto);

        assertThat(result).isSameAs(existingRoute);
        assertThat(result.getRouteSource()).isEqualTo("LOCAL");
        assertThat(result.getRouteProfile()).isEqualTo("foot-hiking");
        assertThat(result.getStartLat()).isEqualByComparingTo("48.3");
        assertThat(result.getRouteGeometry().path("type").asText()).isEqualTo("FeatureCollection");
        assertThat(result.getRouteFetchedAt()).isEqualTo(Instant.parse("2026-06-22T10:00:00Z"));
    }

    @Test
    void applyWeatherCreatesNewWeatherEntityWhenNoneExists() {
        TourEntity tour = minimalTour(1L, 42L);
        TourLogEntity log = fullLog(10L, tour);
        TourLogWeatherDto weatherDto = new TourLogWeatherDto(
                10L, "OPEN_METEO", "archive",
                new CoordinateDto(new BigDecimal("48.2"), new BigDecimal("16.3")),
                Instant.parse("2026-06-21T10:00:00Z"),
                new BigDecimal("20.5"), new BigDecimal("60"),
                new BigDecimal("0.0"), 3, "Overcast",
                new BigDecimal("15.0"), Instant.parse("2026-06-21T11:00:00Z")
        );

        mapper.applyWeather(log, weatherDto);

        assertThat(log.getWeather()).isNotNull();
        assertThat(log.getWeather().getTourLog()).isSameAs(log);
        assertThat(log.getWeather().getProvider()).isEqualTo("OPEN_METEO");
        assertThat(log.getWeather().getTemperatureC()).isEqualByComparingTo("20.5");
        assertThat(log.getWeather().getWeatherCode()).isEqualTo(3);
    }

    @Test
    void applyWeatherClearsWeatherWhenNull() {
        TourEntity tour = minimalTour(1L, 42L);
        TourLogEntity log = fullLog(10L, tour);
        log.setWeather(fullWeather(log));

        mapper.applyWeather(log, null);

        assertThat(log.getWeather()).isNull();
    }

    @Test
    void applyWeatherUpdatesExistingWeatherEntity() {
        TourEntity tour = minimalTour(1L, 42L);
        TourLogEntity log = fullLog(10L, tour);
        log.setWeather(fullWeather(log));
        TourLogWeatherEntity existingWeather = log.getWeather();

        TourLogWeatherDto weatherDto = new TourLogWeatherDto(
                10L, "LOCAL_FALLBACK", "fallback",
                new CoordinateDto(new BigDecimal("48.2"), new BigDecimal("16.3")),
                Instant.parse("2026-06-21T10:00:00Z"),
                new BigDecimal("18.0"), new BigDecimal("55"),
                new BigDecimal("0.5"), 2, "Rain",
                new BigDecimal("20.0"), Instant.parse("2026-06-21T12:00:00Z")
        );

        mapper.applyWeather(log, weatherDto);

        assertThat(log.getWeather()).isSameAs(existingWeather);
        assertThat(log.getWeather().getProvider()).isEqualTo("LOCAL_FALLBACK");
        assertThat(log.getWeather().getTemperatureC()).isEqualByComparingTo("18.0");
        assertThat(log.getWeather().getWeatherDescription()).isEqualTo("Rain");
    }

    @Test
    void applyImportedWeatherClearsWeatherWhenNull() {
        TourEntity tour = minimalTour(1L, 42L);
        TourLogEntity log = fullLog(10L, tour);
        log.setWeather(fullWeather(log));

        mapper.applyImportedWeather(log, null);

        assertThat(log.getWeather()).isNull();
    }

    @Test
    void applyImportedWeatherMapsAllFieldsFromImportDto() {
        TourEntity tour = minimalTour(1L, 42L);
        TourLogEntity log = fullLog(10L, tour);

        ImportedWeatherSnapshotDto importedWeather = new ImportedWeatherSnapshotDto(
                "OPEN_METEO", "archive",
                new CoordinateDto(new BigDecimal("48.2"), new BigDecimal("16.3")),
                Instant.parse("2026-06-21T10:00:00Z"),
                new BigDecimal("22.0"), new BigDecimal("65"),
                new BigDecimal("0.2"), 1, "Clear sky",
                new BigDecimal("10.0"), Instant.parse("2026-06-21T11:00:00Z")
        );

        mapper.applyImportedWeather(log, importedWeather);

        assertThat(log.getWeather()).isNotNull();
        assertThat(log.getWeather().getProvider()).isEqualTo("OPEN_METEO");
        assertThat(log.getWeather().getProviderDataset()).isEqualTo("archive");
        assertThat(log.getWeather().getTemperatureC()).isEqualByComparingTo("22.0");
        assertThat(log.getWeather().getWeatherDescription()).isEqualTo("Clear sky");
    }

    @Test
    void toDomainTransportTypeConvertsCorrectly() {
        assertThat(mapper.toDomainTransportType(TransportType.BIKE))
                .isEqualTo(org.fhtw.mytourapi.domain.TransportType.BIKE);
        assertThat(mapper.toDomainTransportType(TransportType.HIKE))
                .isEqualTo(org.fhtw.mytourapi.domain.TransportType.HIKE);
        assertThat(mapper.toDomainTransportType(TransportType.RUNNING))
                .isEqualTo(org.fhtw.mytourapi.domain.TransportType.RUNNING);
        assertThat(mapper.toDomainTransportType(TransportType.VACATION))
                .isEqualTo(org.fhtw.mytourapi.domain.TransportType.VACATION);
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("alice");
        user.setUsernameNormalized("alice");
        user.setPasswordHash("{noop}password");
        user.setCreatedAt(Instant.parse("2026-06-21T10:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-06-21T10:00:00Z"));
        user.setVersion(0L);
        return user;
    }

    private TourEntity minimalTour(Long id, Long userId) {
        TourEntity tour = new TourEntity();
        tour.setId(id);
        tour.setUser(user(userId));
        tour.setName("Test Tour");
        tour.setDescription("Description");
        tour.setStartLocation("Vienna");
        tour.setEndLocation("Graz");
        tour.setTransportType(org.fhtw.mytourapi.domain.TransportType.BIKE);
        tour.setTimezoneId("Europe/Vienna");
        tour.setPlannedDistanceM(new BigDecimal("12000"));
        tour.setEstimatedDurationS(3600);
        tour.setLogCount(0);
        tour.setPopularityScore(0);
        tour.setPopularityCategory(org.fhtw.mytourapi.domain.PopularityCategory.NEW);
        tour.setPopularityLabel("new");
        tour.setChildFriendlinessScore(0);
        tour.setChildFriendlinessCategory(org.fhtw.mytourapi.domain.ChildFriendlinessCategory.UNKNOWN);
        tour.setChildFriendlinessLabel("unknown");
        tour.setCreatedAt(Instant.parse("2026-06-21T10:00:00Z"));
        tour.setUpdatedAt(Instant.parse("2026-06-21T10:00:00Z"));
        tour.setVersion(1L);
        return tour;
    }

    private TourEntity fullTour(Long id, Long userId) {
        TourEntity tour = minimalTour(id, userId);
        tour.setCoverImagePath("covers/test.jpg");
        tour.setCoverImageOriginalFilename("test.jpg");
        tour.setCoverImageContentType("image/jpeg");
        tour.setCoverImageSizeBytes(1024L);
        tour.setLogCount(3);
        tour.setPopularityScore(2);
        tour.setPopularityCategory(org.fhtw.mytourapi.domain.PopularityCategory.POPULAR);
        tour.setPopularityLabel("popular");
        tour.setChildFriendlinessScore(85);
        tour.setChildFriendlinessCategory(org.fhtw.mytourapi.domain.ChildFriendlinessCategory.FAMILY_FRIENDLY);
        tour.setChildFriendlinessLabel("family friendly");
        tour.setRoute(fullRoute());
        tour.getRoute().setTour(tour);
        return tour;
    }

    private TourRouteEntity fullRoute() {
        TourRouteEntity route = new TourRouteEntity();
        route.setRouteSource("OPENROUTESERVICE");
        route.setRouteProfile("cycling-regular");
        route.setStartLat(new BigDecimal("48.209200"));
        route.setStartLon(new BigDecimal("16.404400"));
        route.setEndLat(new BigDecimal("48.250000"));
        route.setEndLon(new BigDecimal("16.400000"));
        route.setMidpointLat(new BigDecimal("48.229600"));
        route.setMidpointLon(new BigDecimal("16.402200"));
        try {
            route.setRouteGeometry(objectMapper.readTree("{\"type\":\"LineString\"}"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        route.setRouteFetchedAt(Instant.parse("2026-06-21T10:00:00Z"));
        return route;
    }

    private TourLogEntity fullLog(Long id, TourEntity tour) {
        TourLogEntity log = new TourLogEntity();
        log.setId(id);
        log.setTour(tour);
        log.setPerformedAt(Instant.parse("2026-06-21T10:00:00Z"));
        log.setComment("Great tour");
        log.setDifficulty((short) 3);
        log.setTotalDistanceM(new BigDecimal("12000"));
        log.setTotalTimeS(3600);
        log.setRating((short) 4);
        log.setCreatedAt(Instant.parse("2026-06-21T10:05:00Z"));
        log.setUpdatedAt(Instant.parse("2026-06-21T10:05:00Z"));
        log.setVersion(0L);
        return log;
    }

    private TourLogWeatherEntity fullWeather(TourLogEntity log) {
        TourLogWeatherEntity weather = new TourLogWeatherEntity();
        weather.setTourLog(log);
        weather.setProvider("OPEN_METEO");
        weather.setProviderDataset("archive");
        weather.setLookupLat(new BigDecimal("48.229600"));
        weather.setLookupLon(new BigDecimal("16.402200"));
        weather.setWeatherObservedAt(Instant.parse("2026-06-21T10:00:00Z"));
        weather.setTemperatureC(new BigDecimal("20.5"));
        weather.setRelativeHumidityPercent(new BigDecimal("60"));
        weather.setPrecipitationMm(new BigDecimal("0.0"));
        weather.setWeatherCode(3);
        weather.setWeatherDescription("Overcast");
        weather.setWindSpeedKmh(new BigDecimal("15.0"));
        weather.setFetchedAt(Instant.parse("2026-06-21T11:00:00Z"));
        return weather;
    }

    private static Map<String, Object> geoJson() {
        return Map.of("type", "FeatureCollection", "features", List.of());
    }
}
