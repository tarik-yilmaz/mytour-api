package org.fhtw.mytourapi.client;

import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.service.CalculatedRoute;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenRouteServiceDirectionsClientTest {

    @Test
    void fetchRoutePostsCoordinatesAndMapsGeoJsonSummary() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://api.openrouteservice.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        OpenRouteServiceProperties properties = new OpenRouteServiceProperties();
        properties.setApiKey("test-key");
        OpenRouteServiceDirectionsClient client = new OpenRouteServiceDirectionsClient(
                restClientBuilder.build(),
                properties
        );

        server.expect(requestTo("https://api.openrouteservice.org/v2/directions/cycling-regular/geojson"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "test-key"))
                .andExpect(content().json("""
                        {
                          "coordinates": [[16.3738, 48.2082], [16.4000, 48.2500]],
                          "instructions": false
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "type": "FeatureCollection",
                          "features": [
                            {
                              "type": "Feature",
                              "properties": {
                                "summary": {
                                  "distance": 1234.5,
                                  "duration": 678.9
                                }
                              },
                              "geometry": {
                                "type": "LineString",
                                "coordinates": [[16.3738, 48.2082], [16.4000, 48.2500]]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        CalculatedRoute result = client.fetchRoute(
                "cycling-regular",
                coordinate("48.2082", "16.3738"),
                coordinate("48.2500", "16.4000"),
                Instant.parse("2026-06-21T10:00:00Z")
        );

        assertThat(result.distanceM()).isEqualByComparingTo("1234.5");
        assertThat(result.durationS()).isEqualTo(679);
        assertThat(result.route().routeSource()).isEqualTo("OPENROUTESERVICE");
        assertThat(result.route().routeProfile()).isEqualTo("cycling-regular");
        assertThat(result.route().routeGeometry().path("type").asText()).isEqualTo("FeatureCollection");
        server.verify();
    }

    private static CoordinateDto coordinate(String latitude, String longitude) {
        return new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
