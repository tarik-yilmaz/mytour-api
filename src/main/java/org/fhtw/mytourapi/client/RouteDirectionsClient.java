package org.fhtw.mytourapi.client;

import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.service.CalculatedRoute;

import java.time.Instant;

public interface RouteDirectionsClient {

    CalculatedRoute fetchRoute(
            String profile,
            CoordinateDto startCoordinate,
            CoordinateDto endCoordinate,
            Instant fetchedAt
    );
}
