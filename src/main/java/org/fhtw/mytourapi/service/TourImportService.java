package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.dto.ImportResultDto;
import org.fhtw.mytourapi.dto.ImportedTourDto;
import org.fhtw.mytourapi.dto.ImportedTourLogDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourImportRequest;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.exception.ImportValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class TourImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TourImportService.class);
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final IntermediateTourService tourService;
    private final IntermediateTourLogService tourLogService;

    public TourImportService(
            IntermediateTourService tourService,
            IntermediateTourLogService tourLogService
    ) {
        this.tourService = tourService;
        this.tourLogService = tourLogService;
    }

    public ImportResultDto importTours(TourImportRequest request) {
        List<String> validationErrors = validate(request);
        if (!validationErrors.isEmpty()) {
            LOGGER.debug("Rejected tour import validationErrorCount={}", validationErrors.size());
            throw new ImportValidationException(validationErrors);
        }

        List<Long> createdTourIds = new ArrayList<>();
        int importedLogs = 0;

        for (ImportedTourDto importedTour : request.tours()) {
            TourDetailDto createdTour = tourService.importTour(importedTour);
            createdTourIds.add(createdTour.id());
            importedLogs += tourLogService.importLogs(createdTour.id(), importedTour.logs())
                    .orElseThrow()
                    .size();
        }

        LOGGER.info("Imported tours importedTourCount={} importedLogCount={}", createdTourIds.size(), importedLogs);
        return new ImportResultDto(createdTourIds.size(), importedLogs, List.copyOf(createdTourIds));
    }

    private List<String> validate(TourImportRequest request) {
        List<String> errors = new ArrayList<>();
        if (request == null) {
            errors.add("request: must not be null");
            return errors;
        }

        if (request.schemaVersion() == null) {
            errors.add("schemaVersion: must not be null");
        } else if (request.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            errors.add("schemaVersion: unsupported version " + request.schemaVersion()
                    + "; expected " + SUPPORTED_SCHEMA_VERSION);
        }

        if (request.tours() == null || request.tours().isEmpty()) {
            errors.add("tours: must not be empty");
            return errors;
        }

        for (int index = 0; index < request.tours().size(); index++) {
            validateTour(index, request.tours().get(index), errors);
        }

        return errors;
    }

    private void validateTour(int index, ImportedTourDto importedTour, List<String> errors) {
        String path = "tours[" + index + "]";
        if (importedTour == null) {
            errors.add(path + ": must not be null");
            return;
        }

        if (importedTour.tour() == null) {
            errors.add(path + ".tour: must not be null");
        }

        validateRoute(path, importedTour, errors);
        validateCoverImage(path, importedTour.coverImage(), errors);

        if (importedTour.plannedDistanceM() == null) {
            errors.add(path + ".plannedDistanceM: must not be null");
        } else if (importedTour.plannedDistanceM().compareTo(BigDecimal.ZERO) < 0) {
            errors.add(path + ".plannedDistanceM: must be greater than or equal to 0");
        }

        if (importedTour.estimatedDurationS() == null) {
            errors.add(path + ".estimatedDurationS: must not be null");
        } else if (importedTour.estimatedDurationS() <= 0) {
            errors.add(path + ".estimatedDurationS: must be greater than 0");
        }

        if (importedTour.logs() == null) {
            errors.add(path + ".logs: must not be null");
        } else {
            for (int logIndex = 0; logIndex < importedTour.logs().size(); logIndex++) {
                ImportedTourLogDto importedLog = importedTour.logs().get(logIndex);
                if (importedLog == null) {
                    errors.add(path + ".logs[" + logIndex + "]: must not be null");
                } else if (importedLog.log() == null) {
                    errors.add(path + ".logs[" + logIndex + "].log: must not be null");
                }
            }
        }
    }

    private void validateRoute(String path, ImportedTourDto importedTour, List<String> errors) {
        TourRouteDto route = importedTour.route();
        if (route == null) {
            errors.add(path + ".route: must not be null");
            return;
        }

        if (isBlank(route.routeSource())) {
            errors.add(path + ".route.routeSource: must not be blank");
        }
        if (isBlank(route.routeProfile())) {
            errors.add(path + ".route.routeProfile: must not be blank");
        }
        if (route.startCoordinate() == null) {
            errors.add(path + ".route.startCoordinate: must not be null");
        }
        if (route.endCoordinate() == null) {
            errors.add(path + ".route.endCoordinate: must not be null");
        }
        if (route.midpointCoordinate() == null) {
            errors.add(path + ".route.midpointCoordinate: must not be null");
        }
        if (route.routeFetchedAt() == null) {
            errors.add(path + ".route.routeFetchedAt: must not be null");
        }

        if (importedTour.tour() == null) {
            return;
        }

        if (route.startCoordinate() != null
                && !coordinatesMatch(importedTour.tour().startCoordinate(), route.startCoordinate())) {
            errors.add(path + ".route.startCoordinate: must match tour.startCoordinate");
        }
        if (route.endCoordinate() != null
                && !coordinatesMatch(importedTour.tour().endCoordinate(), route.endCoordinate())) {
            errors.add(path + ".route.endCoordinate: must match tour.endCoordinate");
        }
    }

    private void validateCoverImage(String path, CoverImageDto coverImage, List<String> errors) {
        if (coverImage == null) {
            return;
        }

        if (isBlank(coverImage.path())) {
            errors.add(path + ".coverImage.path: must not be blank");
        } else if (unsafeRelativePath(coverImage.path())) {
            errors.add(path + ".coverImage.path: must be a safe relative path");
        }

        if (isBlank(coverImage.originalFilename())) {
            errors.add(path + ".coverImage.originalFilename: must not be blank");
        }
        if (isBlank(coverImage.contentType())) {
            errors.add(path + ".coverImage.contentType: must not be blank");
        }
        if (coverImage.sizeBytes() == null || coverImage.sizeBytes() < 0) {
            errors.add(path + ".coverImage.sizeBytes: must be greater than or equal to 0");
        }
    }

    private static boolean coordinatesMatch(CoordinateDto first, CoordinateDto second) {
        if (first == null || second == null || first.latitude() == null || first.longitude() == null
                || second.latitude() == null || second.longitude() == null) {
            return false;
        }

        return first.latitude().compareTo(second.latitude()) == 0
                && first.longitude().compareTo(second.longitude()) == 0;
    }

    private static boolean unsafeRelativePath(String value) {
        try {
            Path path = Path.of(value);
            Path normalizedPath = path.normalize();
            return path.isAbsolute()
                    || normalizedPath.startsWith("..")
                    || "..".equals(normalizedPath.toString());
        } catch (InvalidPathException exception) {
            return true;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
