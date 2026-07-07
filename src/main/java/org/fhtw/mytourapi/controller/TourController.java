package org.fhtw.mytourapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.dto.CreateTourRequest;
import org.fhtw.mytourapi.dto.ImportResultDto;
import org.fhtw.mytourapi.dto.LocationSuggestionDto;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.TimezoneSuggestionDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourExportDto;
import org.fhtw.mytourapi.dto.TourImportRequest;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TourSearchResponse;
import org.fhtw.mytourapi.dto.TransportType;
import org.fhtw.mytourapi.dto.TourSuggestionDto;
import org.fhtw.mytourapi.dto.UpdateTourRequest;
import org.fhtw.mytourapi.service.TourService;
import org.fhtw.mytourapi.service.LocationSuggestionService;
import org.fhtw.mytourapi.service.TimezoneSuggestionService;
import org.fhtw.mytourapi.service.TourExportService;
import org.fhtw.mytourapi.service.TourImportService;
import org.fhtw.mytourapi.service.StoredCoverImage;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/tours")
@Tag(name = "Tours", description = "CRUD, search, route data, images, import, and export for user-owned tours.")
public class TourController {

    private final TourService tourService;
    private final TourExportService tourExportService;
    private final TourImportService tourImportService;
    private final LocationSuggestionService locationSuggestionService;
    private final TimezoneSuggestionService timezoneSuggestionService;

    public TourController(
            TourService tourService,
            TourExportService tourExportService,
            TourImportService tourImportService,
            LocationSuggestionService locationSuggestionService,
            TimezoneSuggestionService timezoneSuggestionService
    ) {
        this.tourService = tourService;
        this.tourExportService = tourExportService;
        this.tourImportService = tourImportService;
        this.locationSuggestionService = locationSuggestionService;
        this.timezoneSuggestionService = timezoneSuggestionService;
    }

    @GetMapping
    @Operation(summary = "Search and filter tours owned by the authenticated user.")
    public TourSearchResponse searchTours(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TransportType transportType,
            @RequestParam(required = false) PopularityCategory popularity,
            @RequestParam(required = false) ChildFriendlinessCategory childFriendliness,
            @RequestParam(required = false) @Min(1) @Max(5) Short ratingMin
    ) {
        return tourService.searchTours(q, transportType, popularity, childFriendliness, ratingMin);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Suggest user-owned tours for autocomplete search.")
    public List<TourSuggestionDto> suggestTours(
            @RequestParam @Size(max = 120) String q,
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) Integer limit
    ) {
        return tourService.suggestTours(q, limit);
    }

    @GetMapping("/location-suggestions")
    @Operation(summary = "Suggest route locations and coordinates for tour forms.")
    public List<LocationSuggestionDto> suggestLocations(
            @RequestParam @Size(max = 255) String q,
            @RequestParam(defaultValue = "6") @Min(1) @Max(10) Integer limit
    ) {
        return locationSuggestionService.suggestLocations(q, limit);
    }

    @GetMapping("/timezone-suggestions")
    @Operation(summary = "Suggest IANA timezone ids for tour forms.")
    public List<TimezoneSuggestionDto> suggestTimezones(
            @RequestParam(required = false, defaultValue = "") @Size(max = 64) String q,
            @RequestParam(defaultValue = "8") @Min(1) @Max(20) Integer limit
    ) {
        return timezoneSuggestionService.suggestTimezones(q, limit);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a tour and fetch route information from OpenRouteService.")
    public TourDetailDto createTour(@Valid @RequestBody CreateTourRequest request) {
        return tourService.createTour(request);
    }

    @GetMapping("/{tourId}")
    @Operation(summary = "Get one user-owned tour by id.")
    public TourDetailDto getTour(@PathVariable Long tourId) {
        return tourService.getTour(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));
    }

    @PutMapping("/{tourId}")
    @Operation(summary = "Update a tour and refetch route/weather-dependent generated data if needed.")
    public TourDetailDto updateTour(
            @PathVariable Long tourId,
            @Valid @RequestBody UpdateTourRequest request
    ) {
        return tourService.updateTour(tourId, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));
    }

    @DeleteMapping("/{tourId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Physically delete a tour, its route, logs, and weather snapshots.")
    public void deleteTour(@PathVariable Long tourId) {
        if (!tourService.deleteTour(tourId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found");
        }
    }

    @PostMapping("/{tourId}/route/refresh")
    @Operation(summary = "Refetch route data for a tour from OpenRouteService.")
    public TourRouteDto refreshRoute(@PathVariable Long tourId) {
        return tourService.refreshRoute(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));
    }

    @PutMapping(value = "/{tourId}/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace the one cover image for a tour.")
    public CoverImageDto uploadCoverImage(
            @PathVariable Long tourId,
            @RequestPart("file") MultipartFile file
    ) {
        return tourService.uploadCoverImage(tourId, file)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));
    }

    @GetMapping("/{tourId}/cover-image")
    @Operation(summary = "Download the one cover image for a tour.")
    public ResponseEntity<Resource> getCoverImage(@PathVariable Long tourId) {
        StoredCoverImage coverImage = tourService.getCoverImage(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cover image not found"));

        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(coverImage.originalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(coverImage.contentType())
                .contentLength(coverImage.sizeBytes())
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(coverImage.resource());
    }

    @DeleteMapping("/{tourId}/cover-image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete the cover image metadata and filesystem file for a tour.")
    public void deleteCoverImage(@PathVariable Long tourId) {
        if (!tourService.deleteCoverImage(tourId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found");
        }
    }

    @GetMapping("/export")
    @Operation(summary = "Export the authenticated user's tours, route geometry, logs, and weather snapshots.")
    public TourExportDto exportTours() {
        return tourExportService.exportTours();
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Import tours and logs for the authenticated user from the project's JSON export format.")
    public ImportResultDto importTours(@Valid @RequestBody TourImportRequest request) {
        return tourImportService.importTours(request);
    }
}
