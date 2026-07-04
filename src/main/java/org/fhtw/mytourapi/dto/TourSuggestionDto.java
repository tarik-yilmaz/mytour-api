package org.fhtw.mytourapi.dto;

public record TourSuggestionDto(
        Long tourId,
        String label,
        String route,
        String matchedText
) {
}
