package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class IntermediateTourSearchIndex {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SEARCH_CHARACTERS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final Map<Long, SearchDocument> logDocumentsByTourId = new ConcurrentHashMap<>();

    public void replaceLogs(Long tourId, List<TourLogDto> logs) {
        if (tourId == null) {
            return;
        }

        if (logs == null || logs.isEmpty()) {
            logDocumentsByTourId.remove(tourId);
            return;
        }

        logDocumentsByTourId.put(tourId, SearchDocument.fromLogs(logs));
    }

    public void removeTour(Long tourId) {
        if (tourId != null) {
            logDocumentsByTourId.remove(tourId);
        }
    }

    public boolean matches(TourDetailDto tour, String query, Short ratingMin) {
        if (tour == null) {
            return false;
        }

        SearchDocument logDocument = logDocumentsByTourId.getOrDefault(tour.id(), SearchDocument.empty());
        if (!logDocument.hasRatingAtLeast(ratingMin)) {
            return false;
        }

        Set<String> queryTerms = searchTerms(query);
        if (queryTerms.isEmpty()) {
            return true;
        }

        Set<String> documentTerms = new LinkedHashSet<>();
        documentTerms.addAll(searchTerms(tourSearchText(tour)));
        documentTerms.addAll(logDocument.terms());

        return queryTerms.stream()
                .allMatch((queryTerm) -> documentTerms.stream()
                        .anyMatch((documentTerm) -> documentTerm.startsWith(queryTerm)));
    }

    private static String tourSearchText(TourDetailDto tour) {
        SearchTextBuilder builder = new SearchTextBuilder()
                .add(tour.name())
                .add(tour.description())
                .add(tour.startLocation())
                .add(tour.endLocation())
                .add(tour.transportType())
                .add(tour.timezoneId())
                .add(tour.plannedDistanceM())
                .add(tour.estimatedDurationS());

        ComputedTourAttributesDto computedAttributes = tour.computedAttributes();
        if (computedAttributes != null) {
            builder.add("log count")
                    .add(computedAttributes.logCount())
                    .add("popularity")
                    .add(computedAttributes.popularityScore())
                    .add(computedAttributes.popularityCategory())
                    .add(computedAttributes.popularityLabel())
                    .add("child friendliness")
                    .add(computedAttributes.childFriendlinessScore())
                    .add(computedAttributes.childFriendlinessCategory())
                    .add(computedAttributes.childFriendlinessLabel());
        }

        return builder.text();
    }

    private static Set<String> searchTerms(String text) {
        String normalizedText = normalize(text);
        if (normalizedText.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(SPACES.split(normalizedText))
                .filter((term) -> !term.isBlank())
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
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

    private record SearchDocument(Set<String> terms, short maxRating) {

        private static SearchDocument empty() {
            return new SearchDocument(Set.of(), (short) 0);
        }

        private static SearchDocument fromLogs(List<TourLogDto> logs) {
            SearchTextBuilder builder = new SearchTextBuilder();
            short maxRating = 0;

            for (TourLogDto log : logs.stream().filter(Objects::nonNull).toList()) {
                builder.add(log.performedAt())
                        .add(log.comment())
                        .add("difficulty")
                        .add(log.difficulty())
                        .add("distance")
                        .add(log.totalDistanceM())
                        .add("time")
                        .add(log.totalTimeS())
                        .add("rating")
                        .add(log.rating());

                if (log.rating() != null && log.rating() > maxRating) {
                    maxRating = log.rating();
                }

                TourLogWeatherDto weather = log.weather();
                if (weather != null) {
                    builder.add(weather.provider())
                            .add(weather.providerDataset())
                            .add(weather.weatherDescription())
                            .add(weather.weatherCode())
                            .add(weather.temperatureC())
                            .add(weather.relativeHumidityPercent())
                            .add(weather.precipitationMm())
                            .add(weather.windSpeedKmh());
                }
            }

            return new SearchDocument(searchTerms(builder.text()), maxRating);
        }

        private boolean hasRatingAtLeast(Short ratingMin) {
            return ratingMin == null || maxRating >= ratingMin;
        }
    }

    private static final class SearchTextBuilder {

        private final StringBuilder builder = new StringBuilder();

        private SearchTextBuilder add(Object value) {
            if (value != null) {
                builder.append(value).append(' ');
            }

            return this;
        }

        private String text() {
            return builder.toString();
        }
    }
}
