package org.fhtw.mytourapi.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class OpenRouteServiceDirectionsClient implements RouteDirectionsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenRouteServiceDirectionsClient.class);
    private static final String ROUTE_SOURCE = "OPENROUTESERVICE";

    private final RestClient openRouteServiceRestClient;
    private final OpenRouteServiceProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenRouteServiceDirectionsClient(
            @Qualifier("openRouteServiceRestClient") RestClient openRouteServiceRestClient,
            OpenRouteServiceProperties properties
    ) {
        this.openRouteServiceRestClient = openRouteServiceRestClient;
        this.properties = properties;
    }

    @Override
    public RouteDirectionsResult fetchRoute(
            String profile,
            CoordinateDto startCoordinate,
            CoordinateDto endCoordinate,
            Instant fetchedAt
    ) {
        Instant requestStartedAt = Instant.now();
        try {
            String response = openRouteServiceRestClient.post()
                    .uri("/v2/directions/{profile}/geojson", profile)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(new MediaType("application", "geo+json"))
                    .header(HttpHeaders.AUTHORIZATION, properties.getApiKey())
                    .body(requestBody(startCoordinate, endCoordinate))
                    .retrieve()
                    .body(String.class);

            RouteDirectionsResult route = toCalculatedRoute(profile, startCoordinate, endCoordinate, fetchedAt, response);
            LOGGER.info(
                    "OpenRouteService route lookup succeeded profile={} distanceM={} durationS={} latencyMs={}",
                    profile,
                    route.distanceM(),
                    route.durationS(),
                    Duration.between(requestStartedAt, Instant.now()).toMillis()
            );
            return route;
        } catch (RestClientResponseException exception) {
            LOGGER.warn(
                    "OpenRouteService route lookup returned HTTP error profile={} status={} failureClass={}",
                    profile,
                    exception.getStatusCode().value(),
                    exception.getClass().getSimpleName()
            );
            throw responseException(exception);
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "OpenRouteService route lookup failed profile={} failureClass={}",
                    profile,
                    exception.getClass().getSimpleName()
            );
            throw new UpstreamServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenRouteService is currently unavailable.",
                    exception
            );
        }
    }

    RouteDirectionsResult toCalculatedRoute(
            String profile,
            CoordinateDto startCoordinate,
            CoordinateDto endCoordinate,
            Instant fetchedAt,
            String response
    ) {
        JsonNode routeGeometry = parseResponse(response);
        JsonNode firstFeature = routeGeometry.path("features").path(0);
        JsonNode summary = firstFeature.path("properties").path("summary");

        if (!firstFeature.isObject() || !summary.path("distance").isNumber() || !summary.path("duration").isNumber()) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenRouteService returned an unreadable route response."
            );
        }

        BigDecimal distanceM = summary.path("distance").decimalValue();
        Integer durationS = summary.path("duration")
                .decimalValue()
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        TourRouteDto route = new TourRouteDto(
                ROUTE_SOURCE,
                profile,
                startCoordinate,
                endCoordinate,
                midpoint(startCoordinate, endCoordinate),
                routeGeometry,
                fetchedAt
        );

        return new RouteDirectionsResult(route, distanceM, durationS);
    }

    private JsonNode parseResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenRouteService returned an empty route response."
            );
        }

        try {
            JsonNode parsedResponse = objectMapper.readTree(response);
            if (!parsedResponse.isObject()) {
                throw new UpstreamServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenRouteService returned an unreadable route response."
                );
            }

            return parsedResponse;
        } catch (JsonProcessingException exception) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenRouteService returned malformed route JSON.",
                    exception
            );
        }
    }

    private Map<String, Object> requestBody(CoordinateDto startCoordinate, CoordinateDto endCoordinate) {
        return Map.of(
                "coordinates", List.of(
                        longitudeLatitude(startCoordinate),
                        longitudeLatitude(endCoordinate)
                ),
                "instructions", false
        );
    }

    private List<BigDecimal> longitudeLatitude(CoordinateDto coordinate) {
        return List.of(coordinate.longitude(), coordinate.latitude());
    }

    private CoordinateDto midpoint(CoordinateDto startCoordinate, CoordinateDto endCoordinate) {
        return new CoordinateDto(
                startCoordinate.latitude()
                        .add(endCoordinate.latitude())
                        .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP),
                startCoordinate.longitude()
                        .add(endCoordinate.longitude())
                        .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP)
        );
    }

    private UpstreamServiceException responseException(RestClientResponseException exception) {
        if (exception.getStatusCode().is4xxClientError()) {
            return new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenRouteService rejected the route request.",
                    exception
            );
        }

        return new UpstreamServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "OpenRouteService is currently unavailable.",
                exception
        );
    }
}
