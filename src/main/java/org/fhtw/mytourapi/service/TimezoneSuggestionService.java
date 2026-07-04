package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.TimezoneSuggestionDto;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TimezoneSuggestionService {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_SEARCH_CHARACTERS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final List<String> PREFERRED_TIMEZONES = List.of(
            "Europe/Vienna",
            "Europe/Berlin",
            "Europe/Zurich",
            "Europe/London",
            "UTC",
            "America/New_York",
            "Asia/Tokyo",
            "Australia/Sydney"
    );

    private final List<String> timezoneIds;

    public TimezoneSuggestionService() {
        Set<String> orderedIds = new LinkedHashSet<>(PREFERRED_TIMEZONES);
        ZoneId.getAvailableZoneIds().stream()
                .sorted(Comparator.naturalOrder())
                .forEach(orderedIds::add);
        this.timezoneIds = List.copyOf(orderedIds);
    }

    public List<TimezoneSuggestionDto> suggestTimezones(String query, int limit) {
        String normalizedQuery = normalize(query);
        return timezoneIds.stream()
                .filter((timezoneId) -> normalizedQuery.isBlank() || normalize(timezoneId).contains(normalizedQuery))
                .limit(limit)
                .map((timezoneId) -> new TimezoneSuggestionDto(timezoneId, timezoneId))
                .toList();
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
