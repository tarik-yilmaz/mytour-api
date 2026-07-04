package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.client.LocationSuggestionClient;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.LocationSuggestionDto;
import org.fhtw.mytourapi.exception.UpstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class LocationSuggestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationSuggestionService.class);
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SEARCH_CHARACTERS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final List<LocationSuggestionDto> FALLBACK_LOCATIONS = List.of(
            location("Wien Praterstern, Vienna, Austria", "Vienna", "Austria", "48.2189", "16.3927"),
            location("Donauinsel Nord, Vienna, Austria", "Vienna", "Austria", "48.2872", "16.3674"),
            location("Stadtpark, Vienna, Austria", "Vienna", "Austria", "48.2042", "16.3802"),
            location("Rathausplatz, Vienna, Austria", "Vienna", "Austria", "48.2109", "16.3576"),
            location("Nussdorf, Vienna, Austria", "Vienna", "Austria", "48.2601", "16.3688"),
            location("Kahlenberg, Vienna, Austria", "Vienna", "Austria", "48.2767", "16.3339"),
            location("Bad Ischl, Austria", "Bad Ischl", "Austria", "47.7111", "13.6236"),
            location("Hallstatt, Austria", "Hallstatt", "Austria", "47.5622", "13.6493")
    );

    private final OpenRouteServiceProperties properties;
    private final LocationSuggestionClient locationSuggestionClient;

    public LocationSuggestionService(
            OpenRouteServiceProperties properties,
            LocationSuggestionClient locationSuggestionClient
    ) {
        this.properties = properties;
        this.locationSuggestionClient = locationSuggestionClient;
    }

    public List<LocationSuggestionDto> suggestLocations(String query, int limit) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.length() < 2) {
            return List.of();
        }

        if (properties.shouldUseApi()) {
            try {
                List<LocationSuggestionDto> suggestions = locationSuggestionClient.suggestLocations(query.trim(), limit);
                if (!suggestions.isEmpty()) {
                    return suggestions;
                }
            } catch (UpstreamServiceException exception) {
                LOGGER.warn("Location autocomplete fell back to local suggestions status={}", exception.status());
            }
        }

        return FALLBACK_LOCATIONS.stream()
                .filter((suggestion) -> normalize(suggestion.label()).contains(normalizedQuery)
                        || normalize(suggestion.locality()).contains(normalizedQuery))
                .limit(limit)
                .toList();
    }

    private static LocationSuggestionDto location(
            String label,
            String locality,
            String country,
            String latitude,
            String longitude
    ) {
        return new LocationSuggestionDto(
                label,
                locality,
                country,
                new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude)),
                "Europe/Vienna"
        );
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String withoutDiacritics = COMBINING_MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFKD))
                .replaceAll("");
        return NON_SEARCH_CHARACTERS.matcher(withoutDiacritics.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .trim();
    }
}
