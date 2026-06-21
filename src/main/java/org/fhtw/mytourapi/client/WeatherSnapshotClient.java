package org.fhtw.mytourapi.client;

import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;

import java.time.Instant;

public interface WeatherSnapshotClient {

    TourLogWeatherDto fetchWeather(
            Long tourLogId,
            CoordinateDto lookupCoordinate,
            Instant performedAt,
            Instant fetchedAt
    );
}
