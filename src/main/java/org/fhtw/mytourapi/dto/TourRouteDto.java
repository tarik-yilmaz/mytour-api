package org.fhtw.mytourapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Route data generated from OpenRouteService and displayed by Leaflet.")
public record TourRouteDto(
        @Schema(example = "OPENROUTESERVICE")
        String routeSource,

        @Schema(example = "cycling-regular")
        String routeProfile,

        CoordinateDto startCoordinate,
        CoordinateDto endCoordinate,
        CoordinateDto midpointCoordinate,

        @Schema(description = "OpenRouteService GeoJSON stored as PostgreSQL jsonb.")
        Map<String, Object> routeGeometry,

        Instant routeFetchedAt
) {
}
