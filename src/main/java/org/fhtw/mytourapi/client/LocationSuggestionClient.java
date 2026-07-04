package org.fhtw.mytourapi.client;

import org.fhtw.mytourapi.dto.LocationSuggestionDto;

import java.util.List;

public interface LocationSuggestionClient {

    List<LocationSuggestionDto> suggestLocations(String query, int limit);
}
