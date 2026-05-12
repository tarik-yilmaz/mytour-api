package org.fhtw.mytourapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.dto.CreateTourRequest;
import org.fhtw.mytourapi.dto.ImportResultDto;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourExportDto;
import org.fhtw.mytourapi.dto.TourImportRequest;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TourSearchResponse;
import org.fhtw.mytourapi.dto.TransportType;
import org.fhtw.mytourapi.dto.UpdateTourRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/tours")
@Tag(name = "Tours", description = "CRUD, search, route data, images, import, and export for user-owned tours.")
public class TourController {

    @GetMapping
    @Operation(summary = "Search and filter tours owned by the authenticated user.")
    public TourSearchResponse searchTours(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TransportType transportType,
            @RequestParam(required = false) PopularityCategory popularity,
            @RequestParam(required = false) ChildFriendlinessCategory childFriendliness,
            @RequestParam(required = false) @Min(1) @Max(5) Short ratingMin
    ) {
        return notImplemented();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a tour and fetch route information from OpenRouteService.")
    public TourDetailDto createTour(@Valid @RequestBody CreateTourRequest request) {
        return notImplemented();
    }

    @GetMapping("/{tourId}")
    @Operation(summary = "Get one user-owned tour by id.")
    public TourDetailDto getTour(@PathVariable Long tourId) {
        return notImplemented();
    }

    @PutMapping("/{tourId}")
    @Operation(summary = "Update a tour and refetch route/weather-dependent generated data if needed.")
    public TourDetailDto updateTour(
            @PathVariable Long tourId,
            @Valid @RequestBody UpdateTourRequest request
    ) {
        return notImplemented();
    }

    @DeleteMapping("/{tourId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Physically delete a tour, its route, logs, and weather snapshots.")
    public void deleteTour(@PathVariable Long tourId) {
        notImplemented();
    }

    @PostMapping("/{tourId}/route/refresh")
    @Operation(summary = "Refetch route data for a tour from OpenRouteService.")
    public TourRouteDto refreshRoute(@PathVariable Long tourId) {
        return notImplemented();
    }

    @PutMapping(value = "/{tourId}/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace the one cover image for a tour.")
    public CoverImageDto uploadCoverImage(
            @PathVariable Long tourId,
            @RequestPart("file") MultipartFile file
    ) {
        return notImplemented();
    }

    @DeleteMapping("/{tourId}/cover-image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete the cover image metadata and filesystem file for a tour.")
    public void deleteCoverImage(@PathVariable Long tourId) {
        notImplemented();
    }

    @GetMapping("/export")
    @Operation(summary = "Export the authenticated user's tours, route geometry, logs, and weather snapshots.")
    public TourExportDto exportTours() {
        return notImplemented();
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import tours and logs for the authenticated user from the project's JSON export format.")
    public ImportResultDto importTours(@Valid @RequestBody TourImportRequest request) {
        return notImplemented();
    }

    private static <T> T notImplemented() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Tour service is not implemented yet");
    }
}
