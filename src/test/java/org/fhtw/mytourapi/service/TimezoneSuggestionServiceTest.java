package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.TimezoneSuggestionDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimezoneSuggestionServiceTest {

    @Test
    void suggestTimezonesReturnsPreferredMatchesFirst() {
        TimezoneSuggestionService service = new TimezoneSuggestionService();

        assertThat(service.suggestTimezones("vien", 5))
                .extracting(TimezoneSuggestionDto::timezoneId)
                .first()
                .isEqualTo("Europe/Vienna");
        assertThat(service.suggestTimezones("", 3))
                .extracting(TimezoneSuggestionDto::timezoneId)
                .containsExactly("Europe/Vienna", "Europe/Berlin", "Europe/Zurich");
    }
}
