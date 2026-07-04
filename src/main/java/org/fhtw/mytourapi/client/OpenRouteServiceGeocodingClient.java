package org.fhtw.mytourapi.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.LocationSuggestionDto;
import org.fhtw.mytourapi.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class OpenRouteServiceGeocodingClient implements LocationSuggestionClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenRouteServiceGeocodingClient.class);
    private static final Map<String, String> COUNTRY_TIMEZONES = Map.ofEntries(
            Map.entry("AT", "Europe/Vienna"),
            Map.entry("DE", "Europe/Berlin"),
            Map.entry("CH", "Europe/Zurich"),
            Map.entry("IT", "Europe/Rome"),
            Map.entry("FR", "Europe/Paris"),
            Map.entry("GB", "Europe/London"),
            Map.entry("US", "America/New_York")
    );

    private final RestClient openRouteServiceRestClient;
    private final OpenRouteServiceProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenRouteServiceGeocodingClient(
            @Qualifier("openRouteServiceRestClient") RestClient openRouteServiceRestClient,
            OpenRouteServiceProperties properties
    ) {
        this.openRouteServiceRestClient = openRouteServiceRestClient;
        this.properties = properties;
    }

    @Override
    public List<LocationSuggestionDto> suggestLocations(String query, int limit) {
        Instant requestStartedAt = Instant.now();
        try {
            String response = openRouteServiceRestClient.get()
                    .uri((uriBuilder) -> {
                        var builder = uriBuilder.path("/geocode/autocomplete")
                                .queryParam("api_key", properties.getApiKey())
                                .queryParam("text", query)
                                .queryParam("size", limit);
                        if (properties.getGeocodeBoundaryCountry() != null
                                && !properties.getGeocodeBoundaryCountry().isBlank()) {
                            builder.queryParam("boundary.country", properties.getGeocodeBoundaryCountry());
                        }
                        return builder.build();
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            List<LocationSuggestionDto> suggestions = toSuggestions(response, limit);
            LOGGER.info(
                    "OpenRouteService geocode autocomplete succeeded resultCount={} latencyMs={}",
                    suggestions.size(),
                    Duration.between(requestStartedAt, Instant.now()).toMillis()
            );
            return suggestions;
        } catch (RestClientResponseException exception) {
            LOGGER.warn(
                    "OpenRouteService geocode autocomplete returned HTTP error status={} failureClass={}",
                    exception.getStatusCode().value(),
                    exception.getClass().getSimpleName()
            );
            throw responseException(exception);
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "OpenRouteService geocode autocomplete failed failureClass={}",
                    exception.getClass().getSimpleName()
            );
            throw new UpstreamServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenRouteService geocoding is currently unavailable.",
                    exception
            );
        }
    }

    List<LocationSuggestionDto> toSuggestions(String response, int limit) {
        JsonNode features = parseResponse(response).path("features");
        if (!features.isArray()) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenRouteService returned an unreadable geocode response."
            );
        }

        List<LocationSuggestionDto> suggestions = new ArrayList<>();
        for (JsonNode feature : features) {
            toSuggestion(feature).ifPresent(suggestions::add);
            if (suggestions.size() >= limit) {
                break;
            }
        }

        return List.copyOf(suggestions);
    }

    private java.util.Optional<LocationSuggestionDto> toSuggestion(JsonNode feature) {
        JsonNode propertiesNode = feature.path("properties");
        JsonNode coordinates = feature.path("geometry").path("coordinates");
        if (!coordinates.isArray() || coordinates.size() < 2 || !coordinates.path(0).isNumber()
                || !coordinates.path(1).isNumber()) {
            return java.util.Optional.empty();
        }

        String label = firstText(propertiesNode, "label", "name");
        if (label == null || label.isBlank()) {
            return java.util.Optional.empty();
        }

        String countryCode = firstText(propertiesNode, "country_a", "country_code");
        String country = firstText(propertiesNode, "country", "country_a");
        String locality = firstText(propertiesNode, "locality", "county", "region");
        return java.util.Optional.of(new LocationSuggestionDto(
                label,
                locality,
                country,
                new CoordinateDto(coordinates.path(1).decimalValue(), coordinates.path(0).decimalValue()),
                timezoneForCountry(countryCode)
        ));
    }

    private JsonNode parseResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenRouteService returned an empty geocode response."
            );
        }

        try {
            JsonNode parsedResponse = objectMapper.readTree(response);
            if (!parsedResponse.isObject()) {
                throw new UpstreamServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenRouteService returned an unreadable geocode response."
                );
            }

            return parsedResponse;
        } catch (JsonProcessingException exception) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenRouteService returned malformed geocode JSON.",
                    exception
            );
        }
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = node.path(fieldName);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }

        return null;
    }

    private String timezoneForCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return "Europe/Vienna";
        }

        return COUNTRY_TIMEZONES.getOrDefault(countryCode.toUpperCase(Locale.ROOT), "UTC");
    }

    private UpstreamServiceException responseException(RestClientResponseException exception) {
        if (exception.getStatusCode().is4xxClientError()) {
            return new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "OpenRouteService rejected the geocode request.",
                    exception
            );
        }

        return new UpstreamServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "OpenRouteService geocoding is currently unavailable.",
                exception
        );
    }
}
