package org.fhtw.mytourapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ExportedTourDto(
        @NotNull
        @Valid
        CreateTourRequest tour,

        @NotNull
        @Valid
        TourRouteDto route,

        @Valid
        CoverImageDto coverImage,

        @NotNull
        List<@Valid ImportedTourLogDto> logs
) {
}
