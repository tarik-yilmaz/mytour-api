package org.fhtw.mytourapi.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RouteCalculationServiceTest {

    private static final CoordinateDto START = coordinate("48.2082", "16.3738");
    private static final CoordinateDto END = coordinate("48.2500", "16.4000");
    private static final Instant FETCHED_AT = Instant.parse("2026-06-21T10:00:00Z");

    @Test
    void calculateRouteUsesFallbackWhenApiKeyIsMissing() {
        AtomicBoolean clientCalled = new AtomicBoolean(false);
        RouteCalculationService service = new RouteCalculationService(
                new OpenRouteServiceProperties(),
                (profile, startCoordinate, endCoordinate, fetchedAt) -> {
                    clientCalled.set(true);
                    throw new AssertionError("Client must not be called without an API key.");
                }
        );

        CalculatedRoute result = service.calculateRoute(TransportType.BIKE, START, END, FETCHED_AT);

        assertThat(clientCalled).isFalse();
        assertThat(result.distanceM()).isPositive();
        assertThat(result.durationS()).isPositive();
        assertThat(result.route().routeSource()).isEqualTo("INTERMEDIATE");
        assertThat(result.route().routeProfile()).isEqualTo("cycling-regular");
        assertThat(result.route().routeGeometry().path("type").asText()).isEqualTo("FeatureCollection");
    }

    @Test
    void calculateRouteUsesOpenRouteServiceClientWhenApiKeyIsConfigured() {
        OpenRouteServiceProperties properties = new OpenRouteServiceProperties();
        properties.setApiKey("test-key");
        AtomicBoolean clientCalled = new AtomicBoolean(false);
        TourRouteDto route = new TourRouteDto(
                "OPENROUTESERVICE",
                "foot-hiking",
                START,
                END,
                START,
                JsonNodeFactory.instance.objectNode().put("type", "FeatureCollection"),
                FETCHED_AT
        );
        RouteCalculationService service = new RouteCalculationService(
                properties,
                (profile, startCoordinate, endCoordinate, fetchedAt) -> {
                    clientCalled.set(true);
                    assertThat(profile).isEqualTo("foot-hiking");
                    assertThat(startCoordinate).isEqualTo(START);
                    assertThat(endCoordinate).isEqualTo(END);
                    assertThat(fetchedAt).isEqualTo(FETCHED_AT);
                    return new CalculatedRoute(route, new BigDecimal("1234.5"), 987);
                }
        );

        CalculatedRoute result = service.calculateRoute(TransportType.HIKE, START, END, FETCHED_AT);

        assertThat(clientCalled).isTrue();
        assertThat(result.route()).isEqualTo(route);
        assertThat(result.distanceM()).isEqualByComparingTo("1234.5");
        assertThat(result.durationS()).isEqualTo(987);
    }

    private static CoordinateDto coordinate(String latitude, String longitude) {
        return new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
