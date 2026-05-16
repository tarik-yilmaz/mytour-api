package org.fhtw.mytourapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.fhtw.mytourapi.dto.CreateTourLogRequest;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.fhtw.mytourapi.dto.UpdateTourLogRequest;
import org.fhtw.mytourapi.service.IntermediateTourLogService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/tours/{tourId}/logs")
@Tag(name = "Tour logs", description = "CRUD and weather snapshots for accomplished tour statistics.")
public class TourLogController {

    private final IntermediateTourLogService tourLogService;

    public TourLogController(IntermediateTourLogService tourLogService) {
        this.tourLogService = tourLogService;
    }

    @GetMapping
    @Operation(summary = "List all logs for one user-owned tour.")
    public List<TourLogDto> listLogs(@PathVariable Long tourId) {
        return tourLogService.listLogs(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a tour log and fetch its automatic weather snapshot.")
    public TourLogDto createLog(
            @PathVariable Long tourId,
            @Valid @RequestBody CreateTourLogRequest request
    ) {
        return notImplemented();
    }

    @GetMapping("/{logId}")
    @Operation(summary = "Get one log for a user-owned tour.")
    public TourLogDto getLog(
            @PathVariable Long tourId,
            @PathVariable Long logId
    ) {
        return notImplemented();
    }

    @PutMapping("/{logId}")
    @Operation(summary = "Update a tour log and refetch its generated weather snapshot if time or route data changed.")
    public TourLogDto updateLog(
            @PathVariable Long tourId,
            @PathVariable Long logId,
            @Valid @RequestBody UpdateTourLogRequest request
    ) {
        return notImplemented();
    }

    @DeleteMapping("/{logId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Physically delete a tour log and its weather snapshot.")
    public void deleteLog(
            @PathVariable Long tourId,
            @PathVariable Long logId
    ) {
        notImplemented();
    }

    @PostMapping("/{logId}/weather/refresh")
    @Operation(summary = "Refetch and replace the generated Open-Meteo weather snapshot for a tour log.")
    public TourLogWeatherDto refreshWeather(
            @PathVariable Long tourId,
            @PathVariable Long logId
    ) {
        return notImplemented();
    }

    private static <T> T notImplemented() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Tour log service is not implemented yet");
    }
}
