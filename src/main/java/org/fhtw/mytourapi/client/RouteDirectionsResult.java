package org.fhtw.mytourapi.client;

import org.fhtw.mytourapi.dto.TourRouteDto;

import java.math.BigDecimal;

public record RouteDirectionsResult(
        TourRouteDto route,
        BigDecimal distanceM,
        Integer durationS
) {
}
