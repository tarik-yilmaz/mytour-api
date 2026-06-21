package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.ExportedTourDto;
import org.fhtw.mytourapi.dto.TourExportDto;
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
                .map((tour) -> new ExportedTourDto(tour, tourLogService.listLogsForExport(tour.id())))
                .toList();

        return new TourExportDto(SCHEMA_VERSION, Instant.now(), exportedTours);
    }
}
