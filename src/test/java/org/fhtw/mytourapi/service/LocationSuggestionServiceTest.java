package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.client.LocationSuggestionClient;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.LocationSuggestionDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class LocationSuggestionServiceTest {

    @Test
    void suggestLocationsUsesLocalFallbackWhenOpenRouteServiceIsNotConfigured() {
        OpenRouteServiceProperties properties = new OpenRouteServiceProperties();
        properties.setApiKey("");
        AtomicBoolean clientCalled = new AtomicBoolean(false);
        LocationSuggestionService service = new LocationSuggestionService(
                properties,
                clientThatShouldNotBeCalled(clientCalled)
        );

        List<LocationSuggestionDto> suggestions = service.suggestLocations("pra", 5);

        assertThat(clientCalled.get()).isFalse();
        assertThat(suggestions)
                .extracting(LocationSuggestionDto::label)
                .containsExactly("Wien Praterstern, Vienna, Austria");
        assertThat(suggestions.getFirst().coordinate().latitude()).isEqualByComparingTo("48.2189");
        assertThat(suggestions.getFirst().timezoneId()).isEqualTo("Europe/Vienna");
    }

    @Test
    void suggestLocationsRequiresAtLeastTwoCharacters() {
        LocationSuggestionService service = new LocationSuggestionService(
                new OpenRouteServiceProperties(),
                (query, limit) -> List.of()
        );

        assertThat(service.suggestLocations("p", 5)).isEmpty();
    }

    private static LocationSuggestionClient clientThatShouldNotBeCalled(AtomicBoolean clientCalled) {
        return (query, limit) -> {
            clientCalled.set(true);
            return List.of();
        };
    }
}
