package org.fhtw.mytourapi.client;

import org.fhtw.mytourapi.config.OpenMeteoProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoWeatherClientTest {

    @Test
    void fetchWeatherUsesHistoricalHourlyArchiveAndMapsNearestHour() {
        WeatherClientFixture fixture = weatherClientFixture();
        fixture.archiveServer().expect((request) -> {
                    assertThat(request.getURI().getPath()).isEqualTo("/v1/archive");
                    assertThat(request.getURI().getQuery())
                            .contains(
                                    "latitude=48.2530",
                                    "longitude=16.3801",
                                    "start_date=2026-05-10",
                                    "end_date=2026-05-10",
                                    "timezone=UTC",
                                    "temperature_2m",
                                    "relative_humidity_2m",
                                    "precipitation",
                                    "weather_code",
                                    "wind_speed_10m"
                            );
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "hourly": {
                            "time": ["2026-05-10T17:00", "2026-05-10T18:00"],
                            "temperature_2m": [17.1, 18.6],
                            "relative_humidity_2m": [55, 52],
                            "precipitation": [0.2, 0],
                            "weather_code": [3, 2],
                            "wind_speed_10m": [12.4, 11.2]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TourLogWeatherDto weather = fixture.client().fetchWeather(
                101L,
                coordinate("48.2530", "16.3801"),
                Instant.parse("2026-05-10T17:45:00Z"),
                Instant.parse("2026-06-21T10:00:00Z")
        );

        assertThat(weather.provider()).isEqualTo("OPEN_METEO");
        assertThat(weather.providerDataset()).isEqualTo("historical-hourly");
        assertThat(weather.weatherObservedAt()).isEqualTo(Instant.parse("2026-05-10T18:00:00Z"));
        assertThat(weather.temperatureC()).isEqualByComparingTo("18.6");
        assertThat(weather.relativeHumidityPercent()).isEqualByComparingTo("52");
        assertThat(weather.precipitationMm()).isEqualByComparingTo("0");
        assertThat(weather.weatherCode()).isEqualTo(2);
        assertThat(weather.weatherDescription()).isEqualTo("partly cloudy");
        assertThat(weather.windSpeedKmh()).isEqualByComparingTo("11.2");
        fixture.archiveServer().verify();
        fixture.forecastServer().verify();
    }

    @Test
    void fetchWeatherFallsBackToForecastWhenArchiveDoesNotContainTheHour() {
        WeatherClientFixture fixture = weatherClientFixture();
        fixture.archiveServer().expect((request) -> assertThat(request.getURI().getPath()).isEqualTo("/v1/archive"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "hourly": {
                            "time": ["2026-05-10T16:00"],
                            "temperature_2m": [16.1]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        fixture.forecastServer().expect((request) -> assertThat(request.getURI().getPath()).isEqualTo("/v1/forecast"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "hourly": {
                            "time": ["2026-05-10T18:00"],
                            "temperature_2m": [19.2],
                            "relative_humidity_2m": [49],
                            "precipitation": [0],
                            "weather_code": [1],
                            "wind_speed_10m": [7.5]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        TourLogWeatherDto weather = fixture.client().fetchWeather(
                102L,
                coordinate("48.2530", "16.3801"),
                Instant.parse("2026-05-10T17:45:00Z"),
                Instant.parse("2026-06-21T10:00:00Z")
        );

        assertThat(weather.providerDataset()).isEqualTo("forecast-hourly");
        assertThat(weather.weatherDescription()).isEqualTo("mainly clear");
        assertThat(weather.temperatureC()).isEqualByComparingTo("19.2");
        fixture.archiveServer().verify();
        fixture.forecastServer().verify();
    }

    private static WeatherClientFixture weatherClientFixture() {
        RestClient.Builder archiveBuilder = RestClient.builder()
                .baseUrl("https://archive-api.open-meteo.com");
        RestClient.Builder forecastBuilder = RestClient.builder()
                .baseUrl("https://api.open-meteo.com");
        MockRestServiceServer archiveServer = MockRestServiceServer.bindTo(archiveBuilder).build();
        MockRestServiceServer forecastServer = MockRestServiceServer.bindTo(forecastBuilder).build();

        OpenMeteoWeatherClient client = new OpenMeteoWeatherClient(
                archiveBuilder.build(),
                forecastBuilder.build(),
                new OpenMeteoProperties()
        );

        return new WeatherClientFixture(client, archiveServer, forecastServer);
    }

    private static CoordinateDto coordinate(String latitude, String longitude) {
        return new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude));
    }

    private record WeatherClientFixture(
            OpenMeteoWeatherClient client,
            MockRestServiceServer archiveServer,
            MockRestServiceServer forecastServer
    ) {
    }
}
