package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.CreateTourLogRequest;
import org.fhtw.mytourapi.dto.CreateTourRequest;
import org.fhtw.mytourapi.dto.ExportedTourDto;
import org.fhtw.mytourapi.dto.ImportedTourLogDto;
import org.fhtw.mytourapi.dto.ImportedWeatherSnapshotDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourExportDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TourExportService {

    private static final int SCHEMA_VERSION = 1;

    private final IntermediateTourService tourService;
    private final IntermediateTourLogService tourLogService;

    public TourExportService(
            IntermediateTourService tourService,
            IntermediateTourLogService tourLogService
    ) {
        this.tourService = tourService;
        this.tourLogService = tourLogService;
    }

    public TourExportDto exportTours() {
        List<ExportedTourDto> exportedTours = tourService.listToursForExport().stream()
                .map(this::toExportedTour)
                .toList();

        return new TourExportDto(SCHEMA_VERSION, Instant.now(), exportedTours);
    }

    private ExportedTourDto toExportedTour(TourDetailDto tour) {
        TourRouteDto route = tour.route();
        List<ImportedTourLogDto> logs = tourLogService.listLogsForExport(tour.id()).stream()
                .map(this::toImportedLog)
                .toList();

        return new ExportedTourDto(
                new CreateTourRequest(
                        tour.name(),
                        tour.description(),
                        tour.startLocation(),
                        tour.endLocation(),
                        tour.transportType(),
                        tour.timezoneId(),
                        route == null ? null : route.startCoordinate(),
                        route == null ? null : route.endCoordinate()
                ),
                route,
                tour.coverImage(),
                logs
        );
    }

    private ImportedTourLogDto toImportedLog(TourLogDto log) {
        return new ImportedTourLogDto(
                new CreateTourLogRequest(
                        log.performedAt(),
                        log.comment(),
                        log.difficulty(),
                        log.totalDistanceM(),
                        log.totalTimeS(),
                        log.rating()
                ),
                toImportedWeather(log.weather())
        );
    }

    private ImportedWeatherSnapshotDto toImportedWeather(TourLogWeatherDto weather) {
        if (weather == null) {
            return null;
        }

        return new ImportedWeatherSnapshotDto(
                weather.provider(),
                weather.providerDataset(),
                weather.lookupCoordinate(),
                weather.weatherObservedAt(),
                weather.temperatureC(),
                weather.relativeHumidityPercent(),
                weather.precipitationMm(),
                weather.weatherCode(),
                weather.weatherDescription(),
                weather.windSpeedKmh(),
                weather.fetchedAt()
        );
    }
}
