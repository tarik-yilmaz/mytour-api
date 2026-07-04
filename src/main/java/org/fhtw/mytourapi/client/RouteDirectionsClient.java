package org.fhtw.mytourapi.client;

import org.fhtw.mytourapi.dto.CoordinateDto;

import java.time.Instant;

public interface RouteDirectionsClient {

    RouteDirectionsResult fetchRoute(
            String profile,
            CoordinateDto startCoordinate,
            CoordinateDto endCoordinate,
            Instant fetchedAt
    );
}
