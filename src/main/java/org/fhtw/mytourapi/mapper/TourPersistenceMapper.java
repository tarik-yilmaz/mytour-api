package org.fhtw.mytourapi.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fhtw.mytourapi.domain.TourEntity;
import org.fhtw.mytourapi.domain.TourLogEntity;
import org.fhtw.mytourapi.domain.TourLogWeatherEntity;
import org.fhtw.mytourapi.domain.TourRouteEntity;
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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class TourPersistenceMapper {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> JSON_OBJECT_TYPE = new TypeReference<>() {
    };

    public TourDetailDto toDetail(TourEntity tour) {
        return new TourDetailDto(
                tour.getId(),
                tour.getUser().getId(),
                tour.getName(),
                tour.getDescription(),
                tour.getStartLocation(),
                tour.getEndLocation(),
                toDtoTransportType(tour.getTransportType()),
                tour.getTimezoneId(),
                tour.getPlannedDistanceM(),
                tour.getEstimatedDurationS(),
                toCoverImage(tour),
                toRoute(tour.getRoute()),
                toComputedAttributes(tour),
                tour.getCreatedAt(),
                tour.getUpdatedAt(),
                tour.getVersion()
        );
    }

    public TourSummaryDto toSummary(TourEntity tour) {
        return new TourSummaryDto(
                tour.getId(),
                tour.getUser().getId(),
                tour.getName(),
                tour.getStartLocation(),
                tour.getEndLocation(),
                toDtoTransportType(tour.getTransportType()),
                tour.getTimezoneId(),
                tour.getPlannedDistanceM(),
                tour.getEstimatedDurationS(),
                toCoverImage(tour),
                toComputedAttributes(tour),
                tour.getCreatedAt(),
                tour.getUpdatedAt()
        );
    }

    public TourLogDto toLog(TourLogEntity log) {
        return new TourLogDto(
                log.getId(),
                log.getTour().getId(),
                log.getPerformedAt(),
                log.getComment(),
                log.getDifficulty(),
                log.getTotalDistanceM(),
                log.getTotalTimeS(),
                log.getRating(),
                toWeather(log.getWeather()),
                log.getCreatedAt(),
                log.getUpdatedAt(),
                log.getVersion()
        );
    }

    public TourRouteDto toRoute(TourRouteEntity route) {
        if (route == null) {
            return null;
        }

        return new TourRouteDto(
                route.getRouteSource(),
                route.getRouteProfile(),
                coordinate(route.getStartLat(), route.getStartLon()),
                coordinate(route.getEndLat(), route.getEndLon()),
                coordinate(route.getMidpointLat(), route.getMidpointLon()),
                toJsonObject(route.getRouteGeometry()),
                route.getRouteFetchedAt()
        );
    }

    public TourLogWeatherDto toWeather(TourLogWeatherEntity weather) {
        if (weather == null) {
            return null;
        }

        return new TourLogWeatherDto(
                weather.getTourLog().getId(),
                weather.getProvider(),
                weather.getProviderDataset(),
                coordinate(weather.getLookupLat(), weather.getLookupLon()),
                weather.getWeatherObservedAt(),
                weather.getTemperatureC(),
                weather.getRelativeHumidityPercent(),
                weather.getPrecipitationMm(),
                weather.getWeatherCode(),
                weather.getWeatherDescription(),
                weather.getWindSpeedKmh(),
                weather.getFetchedAt()
        );
    }

    public CoverImageDto toCoverImage(TourEntity tour) {
        if (tour.getCoverImagePath() == null) {
            return null;
        }

        return new CoverImageDto(
                tour.getCoverImagePath(),
                tour.getCoverImageOriginalFilename(),
                tour.getCoverImageContentType(),
                tour.getCoverImageSizeBytes()
        );
    }

    public ComputedTourAttributesDto toComputedAttributes(TourEntity tour) {
        return new ComputedTourAttributesDto(
                tour.getLogCount(),
                tour.getPopularityScore(),
                PopularityCategory.valueOf(tour.getPopularityCategory().name()),
                tour.getPopularityLabel(),
                tour.getChildFriendlinessScore(),
                ChildFriendlinessCategory.valueOf(tour.getChildFriendlinessCategory().name()),
                tour.getChildFriendlinessLabel()
        );
    }

    public void applyCoverImage(TourEntity tour, CoverImageDto coverImage) {
        if (coverImage == null) {
            tour.setCoverImagePath(null);
            tour.setCoverImageOriginalFilename(null);
            tour.setCoverImageContentType(null);
            tour.setCoverImageSizeBytes(null);
            return;
        }

        tour.setCoverImagePath(coverImage.path());
        tour.setCoverImageOriginalFilename(coverImage.originalFilename());
        tour.setCoverImageContentType(coverImage.contentType());
        tour.setCoverImageSizeBytes(coverImage.sizeBytes());
    }

    public void applyComputedAttributes(TourEntity tour, ComputedTourAttributesDto computedAttributes) {
        tour.setLogCount(computedAttributes.logCount());
        tour.setPopularityScore(computedAttributes.popularityScore());
        tour.setPopularityCategory(org.fhtw.mytourapi.domain.PopularityCategory.valueOf(
                computedAttributes.popularityCategory().name()
        ));
        tour.setPopularityLabel(computedAttributes.popularityLabel());
        tour.setChildFriendlinessScore(computedAttributes.childFriendlinessScore());
        tour.setChildFriendlinessCategory(org.fhtw.mytourapi.domain.ChildFriendlinessCategory.valueOf(
                computedAttributes.childFriendlinessCategory().name()
        ));
        tour.setChildFriendlinessLabel(computedAttributes.childFriendlinessLabel());
    }

    public TourRouteEntity applyRoute(TourEntity tour, TourRouteDto routeDto) {
        TourRouteEntity route = tour.getRoute();
        if (route == null) {
            route = new TourRouteEntity();
            route.setTour(tour);
            tour.setRoute(route);
        }

        route.setRouteSource(routeDto.routeSource());
        route.setRouteProfile(routeDto.routeProfile());
        route.setStartLat(routeDto.startCoordinate().latitude());
        route.setStartLon(routeDto.startCoordinate().longitude());
        route.setEndLat(routeDto.endCoordinate().latitude());
        route.setEndLon(routeDto.endCoordinate().longitude());
        route.setMidpointLat(routeDto.midpointCoordinate().latitude());
        route.setMidpointLon(routeDto.midpointCoordinate().longitude());
        route.setRouteGeometry(toJsonNode(routeDto.routeGeometry()));
        route.setRouteFetchedAt(routeDto.routeFetchedAt());
        return route;
    }

    public void applyWeather(TourLogEntity log, TourLogWeatherDto weatherDto) {
        if (weatherDto == null) {
            log.setWeather(null);
            return;
        }

        TourLogWeatherEntity weather = log.getWeather();
        if (weather == null) {
            weather = new TourLogWeatherEntity();
            weather.setTourLog(log);
            log.setWeather(weather);
        }

        weather.setProvider(weatherDto.provider());
        weather.setProviderDataset(weatherDto.providerDataset());
        weather.setLookupLat(weatherDto.lookupCoordinate().latitude());
        weather.setLookupLon(weatherDto.lookupCoordinate().longitude());
        weather.setWeatherObservedAt(weatherDto.weatherObservedAt());
        weather.setTemperatureC(weatherDto.temperatureC());
        weather.setRelativeHumidityPercent(weatherDto.relativeHumidityPercent());
        weather.setPrecipitationMm(weatherDto.precipitationMm());
        weather.setWeatherCode(weatherDto.weatherCode());
        weather.setWeatherDescription(weatherDto.weatherDescription());
        weather.setWindSpeedKmh(weatherDto.windSpeedKmh());
        weather.setFetchedAt(weatherDto.fetchedAt());
    }

    public void applyImportedWeather(TourLogEntity log, ImportedWeatherSnapshotDto weatherDto) {
        if (weatherDto == null) {
            log.setWeather(null);
            return;
        }

        applyWeather(log, new TourLogWeatherDto(
                log.getId(),
                weatherDto.provider(),
                weatherDto.providerDataset(),
                weatherDto.lookupCoordinate(),
                weatherDto.weatherObservedAt(),
                weatherDto.temperatureC(),
                weatherDto.relativeHumidityPercent(),
                weatherDto.precipitationMm(),
                weatherDto.weatherCode(),
                weatherDto.weatherDescription(),
                weatherDto.windSpeedKmh(),
                weatherDto.fetchedAt()
        ));
    }

    public org.fhtw.mytourapi.domain.TransportType toDomainTransportType(TransportType transportType) {
        return org.fhtw.mytourapi.domain.TransportType.valueOf(transportType.name());
    }

    private TransportType toDtoTransportType(org.fhtw.mytourapi.domain.TransportType transportType) {
        return TransportType.valueOf(transportType.name());
    }

    private CoordinateDto coordinate(BigDecimal latitude, BigDecimal longitude) {
        return new CoordinateDto(latitude, longitude);
    }

    private Map<String, Object> toJsonObject(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }

        return JSON_MAPPER.convertValue(jsonNode, JSON_OBJECT_TYPE);
    }

    private JsonNode toJsonNode(Map<String, Object> jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        return JSON_MAPPER.valueToTree(jsonObject);
    }
}
