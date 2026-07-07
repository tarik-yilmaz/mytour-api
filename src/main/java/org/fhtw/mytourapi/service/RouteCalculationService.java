package org.fhtw.mytourapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.fhtw.mytourapi.client.RouteDirectionsClient;
import org.fhtw.mytourapi.client.RouteDirectionsResult;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class RouteCalculationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteCalculationService.class);
    private static final String FALLBACK_ROUTE_SOURCE = "LOCAL";

    private final OpenRouteServiceProperties properties;
    private final RouteDirectionsClient directionsClient;

    public RouteCalculationService(
            OpenRouteServiceProperties properties,
            RouteDirectionsClient directionsClient
    ) {
        this.properties = properties;
        this.directionsClient = directionsClient;
    }

    public CalculatedRoute calculateRoute(
            TransportType transportType,
            CoordinateDto startCoordinate,
            CoordinateDto endCoordinate,
            Instant fetchedAt
    ) {
        String profile = properties.profileFor(transportType);
        if (properties.shouldUseApi()) {
            LOGGER.debug("Delegating route calculation to OpenRouteService transportType={} profile={}", transportType, profile);
            RouteDirectionsResult route = directionsClient.fetchRoute(profile, startCoordinate, endCoordinate, fetchedAt);
            return new CalculatedRoute(route.route(), route.distanceM(), route.durationS());
        }

        LOGGER.info("Using local fallback route calculation transportType={} profile={}", transportType, profile);
        return fallbackRoute(profile, transportType, startCoordinate, endCoordinate, fetchedAt);
    }

    private CalculatedRoute fallbackRoute(
            String profile,
            TransportType transportType,
            CoordinateDto startCoordinate,
            CoordinateDto endCoordinate,
            Instant fetchedAt
    ) {
        BigDecimal distanceM = calculateDistanceM(startCoordinate, endCoordinate);
        Integer durationS = estimateDurationS(distanceM, transportType);
        CoordinateDto midpoint = midpoint(startCoordinate, endCoordinate);
        JsonNode geometry = fallbackGeoJson(startCoordinate, endCoordinate, distanceM, durationS);
        TourRouteDto route = new TourRouteDto(
                FALLBACK_ROUTE_SOURCE,
                profile,
                startCoordinate,
                endCoordinate,
                midpoint,
                geometry,
                fetchedAt
        );

        return new CalculatedRoute(route, distanceM, durationS);
    }

    private JsonNode fallbackGeoJson(
            CoordinateDto startCoordinate,
            CoordinateDto endCoordinate,
            BigDecimal distanceM,
            Integer durationS
    ) {
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ObjectNode featureCollection = factory.objectNode();
        featureCollection.put("type", "FeatureCollection");

        ArrayNode features = featureCollection.putArray("features");
        ObjectNode feature = features.addObject();
        feature.put("type", "Feature");

        ObjectNode propertiesNode = feature.putObject("properties");
        ObjectNode summary = propertiesNode.putObject("summary");
        summary.put("distance", distanceM);
        summary.put("duration", durationS);

        ObjectNode geometry = feature.putObject("geometry");
        geometry.put("type", "LineString");
        ArrayNode coordinates = geometry.putArray("coordinates");
        addLongitudeLatitude(coordinates, startCoordinate);
        addLongitudeLatitude(coordinates, endCoordinate);

        return featureCollection;
    }

    private void addLongitudeLatitude(ArrayNode coordinates, CoordinateDto coordinate) {
        ArrayNode coordinateNode = coordinates.addArray();
        coordinateNode.add(coordinate.longitude());
        coordinateNode.add(coordinate.latitude());
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

    private BigDecimal calculateDistanceM(CoordinateDto startCoordinate, CoordinateDto endCoordinate) {
        double earthRadiusM = 6_371_000.0;
        double startLat = Math.toRadians(startCoordinate.latitude().doubleValue());
        double endLat = Math.toRadians(endCoordinate.latitude().doubleValue());
        double deltaLat = Math.toRadians(endCoordinate.latitude().subtract(startCoordinate.latitude()).doubleValue());
        double deltaLon = Math.toRadians(endCoordinate.longitude().subtract(startCoordinate.longitude()).doubleValue());

        double haversine = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(startLat) * Math.cos(endLat) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));

        return BigDecimal.valueOf(Math.round(earthRadiusM * angularDistance));
    }

    private Integer estimateDurationS(BigDecimal distanceM, TransportType transportType) {
        double speedMetersPerSecond = switch (transportType) {
            case BIKE -> 4.8;
            case HIKE -> 1.25;
            case RUNNING -> 2.8;
            case VACATION -> 8.0;
        };

        return Math.max(60, (int) Math.round(distanceM.doubleValue() / speedMetersPerSecond));
    }
}
