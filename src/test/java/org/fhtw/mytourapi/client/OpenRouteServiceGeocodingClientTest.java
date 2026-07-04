package org.fhtw.mytourapi.client;

import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.LocationSuggestionDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenRouteServiceGeocodingClientTest {

    @Test
    void suggestLocationsCallsAutocompleteAndMapsCoordinates() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://api.openrouteservice.org");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        OpenRouteServiceProperties properties = new OpenRouteServiceProperties();
        properties.setApiKey("test-key");
        properties.setGeocodeBoundaryCountry("AT");
        OpenRouteServiceGeocodingClient client = new OpenRouteServiceGeocodingClient(
                restClientBuilder.build(),
                properties
        );

        server.expect((request) -> {
                    assertThat(request.getURI().getPath()).isEqualTo("/geocode/autocomplete");
                    assertThat(request.getURI().getQuery())
                            .contains("api_key=test-key", "text=Prater", "size=3", "boundary.country=AT");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "features": [
                            {
                              "geometry": {
                                "coordinates": [16.3927, 48.2189]
                              },
                              "properties": {
                                "label": "Wien Praterstern, Vienna, Austria",
                                "locality": "Vienna",
                                "country": "Austria",
                                "country_a": "AT"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<LocationSuggestionDto> suggestions = client.suggestLocations("Prater", 3);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.getFirst().label()).isEqualTo("Wien Praterstern, Vienna, Austria");
        assertThat(suggestions.getFirst().coordinate().latitude()).isEqualByComparingTo("48.2189");
        assertThat(suggestions.getFirst().coordinate().longitude()).isEqualByComparingTo("16.3927");
        assertThat(suggestions.getFirst().timezoneId()).isEqualTo("Europe/Vienna");
        server.verify();
    }
}
