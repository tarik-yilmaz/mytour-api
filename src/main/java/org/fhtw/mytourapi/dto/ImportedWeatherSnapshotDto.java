package org.fhtw.mytourapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record ImportedWeatherSnapshotDto(
        @NotBlank
        @Size(max = 50)
        String provider,

        @Size(max = 100)
        String providerDataset,

        @NotNull
        @Valid
        CoordinateDto lookupCoordinate,

        @NotNull
        Instant weatherObservedAt,

        BigDecimal temperatureC,
        BigDecimal relativeHumidityPercent,
        BigDecimal precipitationMm,
        Integer weatherCode,

        @Size(max = 120)
        String weatherDescription,

        BigDecimal windSpeedKmh,

        Instant fetchedAt
) {
}
