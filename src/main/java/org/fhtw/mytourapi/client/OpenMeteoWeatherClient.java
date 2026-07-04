package org.fhtw.mytourapi.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fhtw.mytourapi.config.OpenMeteoProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Component
public class OpenMeteoWeatherClient implements WeatherSnapshotClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenMeteoWeatherClient.class);
    private static final String PROVIDER = "OPEN_METEO";
    private static final String ARCHIVE_DATASET = "historical-hourly";
    private static final String FORECAST_DATASET = "forecast-hourly";
    private static final String HOURLY_VARIABLES =
            "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m";

    private final RestClient archiveRestClient;
    private final RestClient forecastRestClient;
    private final OpenMeteoProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenMeteoWeatherClient(
            @Qualifier("openMeteoArchiveRestClient") RestClient archiveRestClient,
            @Qualifier("openMeteoForecastRestClient") RestClient forecastRestClient,
            OpenMeteoProperties properties
    ) {
        this.archiveRestClient = archiveRestClient;
        this.forecastRestClient = forecastRestClient;
        this.properties = properties;
    }

    @Override
    public TourLogWeatherDto fetchWeather(
            Long tourLogId,
            CoordinateDto lookupCoordinate,
            Instant performedAt,
            Instant fetchedAt
    ) {
        Instant observedAt = nearestHour(performedAt);
        UpstreamServiceException archiveFailure = null;

        if (!observedAt.isAfter(fetchedAt)) {
            try {
                return fetchFrom(archiveRestClient, "/v1/archive", ARCHIVE_DATASET, tourLogId, lookupCoordinate, observedAt, fetchedAt);
            } catch (UpstreamServiceException exception) {
                LOGGER.info(
                        "Open-Meteo archive lookup failed; trying eligible fallback if available tourLogId={} observedAt={} status={}",
                        tourLogId,
                        observedAt,
                        exception.status()
                );
                archiveFailure = exception;
            }
        }

        if (isForecastWindow(observedAt, fetchedAt)) {
            try {
                return fetchFrom(forecastRestClient, "/v1/forecast", FORECAST_DATASET, tourLogId, lookupCoordinate, observedAt, fetchedAt);
            } catch (UpstreamServiceException exception) {
                if (archiveFailure == null) {
                    throw exception;
                }
                LOGGER.warn(
                        "Open-Meteo forecast fallback failed after archive failure tourLogId={} observedAt={} archiveStatus={} forecastStatus={}",
                        tourLogId,
                        observedAt,
                        archiveFailure.status(),
                        exception.status()
                );
            }
        }

        if (archiveFailure != null) {
            throw archiveFailure;
        }

        throw new UpstreamServiceException(
                HttpStatus.BAD_GATEWAY,
                "Open-Meteo did not return weather data for the requested tour log hour."
        );
    }

    private TourLogWeatherDto fetchFrom(
            RestClient restClient,
            String path,
            String dataset,
            Long tourLogId,
            CoordinateDto lookupCoordinate,
            Instant observedAt,
            Instant fetchedAt
    ) {
        LocalDate observedDate = LocalDateTime.ofInstant(observedAt, ZoneOffset.UTC).toLocalDate();
        Instant requestStartedAt = Instant.now();

        try {
            String response = restClient.get()
                    .uri((uriBuilder) -> uriBuilder.path(path)
                            .queryParam("latitude", lookupCoordinate.latitude())
                            .queryParam("longitude", lookupCoordinate.longitude())
                            .queryParam("start_date", observedDate)
                            .queryParam("end_date", observedDate)
                            .queryParam("hourly", HOURLY_VARIABLES)
                            .queryParam("timezone", "UTC")
                            .queryParam("timeformat", "iso8601")
                            .queryParam("temperature_unit", "celsius")
                            .queryParam("wind_speed_unit", "kmh")
                            .queryParam("precipitation_unit", "mm")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            TourLogWeatherDto weather = toWeatherSnapshot(dataset, tourLogId, lookupCoordinate, observedAt, fetchedAt, response);
            LOGGER.info(
                    "Open-Meteo weather lookup succeeded dataset={} tourLogId={} observedAt={} latencyMs={}",
                    dataset,
                    tourLogId,
                    observedAt,
                    Duration.between(requestStartedAt, Instant.now()).toMillis()
            );
            return weather;
        } catch (RestClientResponseException exception) {
            LOGGER.warn(
                    "Open-Meteo weather lookup returned HTTP error dataset={} tourLogId={} status={} failureClass={}",
                    dataset,
                    tourLogId,
                    exception.getStatusCode().value(),
                    exception.getClass().getSimpleName()
            );
            throw responseException(exception);
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Open-Meteo weather lookup failed dataset={} tourLogId={} failureClass={}",
                    dataset,
                    tourLogId,
                    exception.getClass().getSimpleName()
            );
            throw new UpstreamServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Open-Meteo is currently unavailable.",
                    exception
            );
        }
    }

    TourLogWeatherDto toWeatherSnapshot(
            String dataset,
            Long tourLogId,
            CoordinateDto lookupCoordinate,
            Instant observedAt,
            Instant fetchedAt,
            String response
    ) {
        JsonNode hourly = parseResponse(response).path("hourly");
        JsonNode times = hourly.path("time");
        if (!times.isArray()) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Open-Meteo returned an unreadable weather response."
            );
        }

        int hourIndex = findHourIndex(times, observedAt);
        if (hourIndex < 0) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Open-Meteo did not return the requested weather hour."
            );
        }

        Integer weatherCode = integerAt(hourly, "weather_code", hourIndex);
        return new TourLogWeatherDto(
                tourLogId,
                PROVIDER,
                dataset,
                lookupCoordinate,
                observedAt,
                decimalAt(hourly, "temperature_2m", hourIndex),
                decimalAt(hourly, "relative_humidity_2m", hourIndex),
                decimalAt(hourly, "precipitation", hourIndex),
                weatherCode,
                weatherDescription(weatherCode),
                decimalAt(hourly, "wind_speed_10m", hourIndex),
                fetchedAt
        );
    }

    private JsonNode parseResponse(String response) {
        if (response == null || response.isBlank()) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Open-Meteo returned an empty weather response."
            );
        }

        try {
            JsonNode parsedResponse = objectMapper.readTree(response);
            if (!parsedResponse.isObject()) {
                throw new UpstreamServiceException(
                        HttpStatus.BAD_GATEWAY,
                        "Open-Meteo returned an unreadable weather response."
                );
            }

            return parsedResponse;
        } catch (JsonProcessingException exception) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Open-Meteo returned malformed weather JSON.",
                    exception
            );
        }
    }

    private int findHourIndex(JsonNode times, Instant observedAt) {
        for (int index = 0; index < times.size(); index++) {
            JsonNode time = times.get(index);
            if (time != null && time.isTextual() && observedAt.equals(parseHour(time.asText()))) {
                return index;
            }
        }

        return -1;
    }

    private Instant parseHour(String value) {
        try {
            if (value.endsWith("Z")) {
                return Instant.parse(value);
            }

            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException exception) {
            throw new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Open-Meteo returned an unreadable weather timestamp.",
                    exception
            );
        }
    }

    private BigDecimal decimalAt(JsonNode hourly, String fieldName, int index) {
        JsonNode value = hourly.path(fieldName).path(index);
        return value.isNumber() ? value.decimalValue() : null;
    }

    private Integer integerAt(JsonNode hourly, String fieldName, int index) {
        JsonNode value = hourly.path(fieldName).path(index);
        return value.isInt() ? value.intValue() : null;
    }

    private boolean isForecastWindow(Instant observedAt, Instant fetchedAt) {
        LocalDate observedDate = LocalDateTime.ofInstant(observedAt, ZoneOffset.UTC).toLocalDate();
        LocalDate today = LocalDateTime.ofInstant(fetchedAt, ZoneOffset.UTC).toLocalDate();

        return !observedDate.isBefore(today.minusDays(properties.getForecastPastDays()))
                && !observedDate.isAfter(today.plusDays(properties.getForecastDays()));
    }

    private Instant nearestHour(Instant performedAt) {
        Instant truncated = performedAt.truncatedTo(ChronoUnit.HOURS);
        long minutes = ChronoUnit.MINUTES.between(truncated, performedAt);
        return minutes >= 30 ? truncated.plus(1, ChronoUnit.HOURS) : truncated;
    }

    private String weatherDescription(Integer weatherCode) {
        if (weatherCode == null) {
            return null;
        }

        return switch (weatherCode) {
            case 0 -> "clear sky";
            case 1 -> "mainly clear";
            case 2 -> "partly cloudy";
            case 3 -> "overcast";
            case 45 -> "fog";
            case 48 -> "depositing rime fog";
            case 51 -> "light drizzle";
            case 53 -> "moderate drizzle";
            case 55 -> "dense drizzle";
            case 56 -> "light freezing drizzle";
            case 57 -> "dense freezing drizzle";
            case 61 -> "slight rain";
            case 63 -> "moderate rain";
            case 65 -> "heavy rain";
            case 66 -> "light freezing rain";
            case 67 -> "heavy freezing rain";
            case 71 -> "slight snowfall";
            case 73 -> "moderate snowfall";
            case 75 -> "heavy snowfall";
            case 77 -> "snow grains";
            case 80 -> "slight rain showers";
            case 81 -> "moderate rain showers";
            case 82 -> "violent rain showers";
            case 85 -> "slight snow showers";
            case 86 -> "heavy snow showers";
            case 95 -> "thunderstorm";
            case 96 -> "thunderstorm with slight hail";
            case 99 -> "thunderstorm with heavy hail";
            default -> "weather code " + weatherCode;
        };
    }

    private UpstreamServiceException responseException(RestClientResponseException exception) {
        if (exception.getStatusCode().is4xxClientError()) {
            return new UpstreamServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Open-Meteo rejected the weather request.",
                    exception
            );
        }

        return new UpstreamServiceException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Open-Meteo is currently unavailable.",
                exception
        );
    }
}
