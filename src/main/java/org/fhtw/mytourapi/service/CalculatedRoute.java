package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.TourRouteDto;

import java.math.BigDecimal;

public record CalculatedRoute(
        TourRouteDto route,
        BigDecimal distanceM,
        Integer durationS
) {
}
