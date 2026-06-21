package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.config.OpenMeteoProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.fhtw.mytourapi.exception.UpstreamServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherSnapshotServiceTest {

    @Test
    void snapshotForDelegatesToConfiguredWeatherClientWithRouteMidpoint() {
        AtomicReference<CoordinateDto> lookupCoordinate = new AtomicReference<>();
        OpenMeteoProperties properties = new OpenMeteoProperties();
        WeatherSnapshotService service = new WeatherSnapshotService(
                properties,
                (tourLogId, lookup, performedAt, fetchedAt) -> {
                    lookupCoordinate.set(lookup);
                    return new TourLogWeatherDto(
                            tourLogId,
                            "OPEN_METEO",
                            "historical-hourly",
                            lookup,
                            Instant.parse("2026-05-10T18:00:00Z"),
                            new BigDecimal("18.6"),
                            new BigDecimal("52"),
                            BigDecimal.ZERO,
                            2,
                            "partly cloudy",
                            new BigDecimal("11.2"),
                            fetchedAt
                    );
                }
        );

        TourLogWeatherDto weather = service.snapshotFor(
                101L,
                tour(),
                Instant.parse("2026-05-10T17:45:00Z"),
                Instant.parse("2026-06-21T10:00:00Z")
        );

        assertThat(lookupCoordinate.get()).isEqualTo(coordinate("48.2530", "16.3801"));
        assertThat(weather.provider()).isEqualTo("OPEN_METEO");
        assertThat(weather.providerDataset()).isEqualTo("historical-hourly");
    }

    @Test
    void snapshotForUsesLocalFallbackWhenOpenMeteoFails() {
        OpenMeteoProperties properties = new OpenMeteoProperties();
        WeatherSnapshotService service = new WeatherSnapshotService(
                properties,
                (tourLogId, lookupCoordinate, performedAt, fetchedAt) -> {
                    throw new UpstreamServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Open-Meteo unavailable");
                }
        );

        TourLogWeatherDto weather = service.snapshotFor(
                102L,
                tour(),
                Instant.parse("2026-05-10T17:45:00Z"),
                Instant.parse("2026-06-21T10:00:00Z")
        );

        assertThat(weather.provider()).isEqualTo("LOCAL_FALLBACK");
        assertThat(weather.providerDataset()).isEqualTo("deterministic-hourly");
        assertThat(weather.lookupCoordinate()).isEqualTo(coordinate("48.2530", "16.3801"));
        assertThat(weather.weatherObservedAt()).isEqualTo(Instant.parse("2026-05-10T18:00:00Z"));
        assertThat(weather.fetchedAt()).isEqualTo(Instant.parse("2026-06-21T10:00:00Z"));
    }

    private static TourDetailDto tour() {
        return new TourDetailDto(
                1L,
                1L,
                "Danube Ride",
                "Evening route",
                "Wien Praterstern",
                "Donauinsel Nord",
                TransportType.BIKE,
                "Europe/Vienna",
                new BigDecimal("18200"),
                4200,
                null,
                new TourRouteDto(
                        "OPENROUTESERVICE",
                        "cycling-regular",
                        coordinate("48.2189", "16.3927"),
                        coordinate("48.2872", "16.3674"),
                        coordinate("48.2530", "16.3801"),
                        null,
                        Instant.parse("2026-05-10T17:30:00Z")
                ),
                null,
                Instant.parse("2026-04-02T08:15:00Z"),
                Instant.parse("2026-05-10T17:30:00Z"),
                1L
        );
    }

    private static CoordinateDto coordinate(String latitude, String longitude) {
        return new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
