package org.fhtw.mytourapi.dto;

public record LocationSuggestionDto(
        String label,
        String locality,
        String country,
        CoordinateDto coordinate,
        String timezoneId
) {
}
