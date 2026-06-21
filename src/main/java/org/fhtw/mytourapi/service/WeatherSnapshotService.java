package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.client.WeatherSnapshotClient;
import org.fhtw.mytourapi.config.OpenMeteoProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.fhtw.mytourapi.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class WeatherSnapshotService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeatherSnapshotService.class);
    private static final String FALLBACK_PROVIDER = "LOCAL_FALLBACK";
    private static final String FALLBACK_DATASET = "deterministic-hourly";

    private final OpenMeteoProperties properties;
    private final WeatherSnapshotClient weatherClient;

    public WeatherSnapshotService(
            OpenMeteoProperties properties,
            WeatherSnapshotClient weatherClient
    ) {
        this.properties = properties;
        this.weatherClient = weatherClient;
    }

    public TourLogWeatherDto snapshotFor(
            Long tourLogId,
            TourDetailDto tour,
            Instant performedAt,
            Instant fetchedAt
    ) {
        CoordinateDto lookupCoordinate = lookupCoordinate(tour);
        if (properties.shouldUseApi()) {
            try {
                return weatherClient.fetchWeather(tourLogId, lookupCoordinate, performedAt, fetchedAt);
            } catch (UpstreamServiceException exception) {
                LOGGER.warn(
                        "Weather snapshot lookup failed for tourLogId={} provider=open-meteo dataset=fallback status={}",
                        tourLogId,
                        exception.status()
                );
            }
        }

        return fallbackWeather(tourLogId, lookupCoordinate, performedAt, fetchedAt);
    }

    static WeatherSnapshotService localFallback() {
        OpenMeteoProperties properties = new OpenMeteoProperties();
        properties.setEnabled(false);

        return new WeatherSnapshotService(
                properties,
                (tourLogId, lookupCoordinate, performedAt, fetchedAt) -> {
                    throw new IllegalStateException("Weather client is disabled for local fallback.");
                }
        );
    }

    private TourLogWeatherDto fallbackWeather(
            Long tourLogId,
            CoordinateDto lookupCoordinate,
            Instant performedAt,
            Instant fetchedAt
    ) {
        Instant observedAt = nearestHour(performedAt);
        long hourBucket = Math.floorMod(observedAt.getEpochSecond() / 3600, 24);
        String description = switch ((int) (hourBucket % 4)) {
            case 0 -> "clear sky";
            case 1 -> "partly cloudy";
            case 2 -> "cloudy";
            default -> "light breeze";
        };

        return new TourLogWeatherDto(
                tourLogId,
                FALLBACK_PROVIDER,
                FALLBACK_DATASET,
                lookupCoordinate,
                observedAt,
                BigDecimal.valueOf(9.5 + (hourBucket % 10) * 1.1),
                BigDecimal.valueOf(48 + Math.floorMod(hourBucket * 3, 35)),
                BigDecimal.ZERO,
                (int) (hourBucket % 4),
                description,
                BigDecimal.valueOf(6.0 + (hourBucket % 7) * 1.4),
                fetchedAt
        );
    }

    private CoordinateDto lookupCoordinate(TourDetailDto tour) {
        if (tour.route() == null || tour.route().midpointCoordinate() == null) {
            return new CoordinateDto(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        return tour.route().midpointCoordinate();
    }

    private Instant nearestHour(Instant performedAt) {
        Instant truncated = performedAt.truncatedTo(ChronoUnit.HOURS);
        long minutes = ChronoUnit.MINUTES.between(truncated, performedAt);
        return minutes >= 30 ? truncated.plus(1, ChronoUnit.HOURS) : truncated;
    }
}
