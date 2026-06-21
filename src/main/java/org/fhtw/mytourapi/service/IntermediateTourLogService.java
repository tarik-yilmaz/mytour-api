package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CreateTourLogRequest;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.fhtw.mytourapi.dto.UpdateTourLogRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IntermediateTourLogService {

    private final IntermediateTourService tourService;
    private final Map<Long, List<TourLogDto>> logsByTourId = new ConcurrentHashMap<>(Map.of(
            1L, List.of(
                    log(101L, 1L, "2026-05-10T17:45:00Z", "Calm evening ride, light wind, good route for beginners.", 2, "18400", 4380, 5,
                            weather(101L, "2026-05-10T18:00:00Z", "18.6", "52", "0", "clear sky", "11.2")),
                    log(102L, 1L, "2026-05-12T18:10:00Z", "Short stop at the river, still easy to keep a steady pace.", 2, "18150", 4260, 4,
                            weather(102L, "2026-05-12T18:00:00Z", "17.9", "58", "0", "partly cloudy", "9.4")),
                    log(103L, 1L, "2026-05-15T17:30:00Z", "A bit crowded near the station, but the island section was excellent.", 3, "18300", 4560, 4,
                            weather(103L, "2026-05-15T18:00:00Z", "20.1", "49", "0", "clear sky", "8.1"))
            ),
            2L, List.of(
                    log(201L, 2L, "2026-05-03T05:45:00Z", "Steady climb before sunrise. Great view, but not ideal for small children.", 4, "7900", 8580, 5,
                            weather(201L, "2026-05-03T06:00:00Z", "9.8", "67", "0", "cloudy", "13.0"))
            ),
            3L, List.of(
                    log(301L, 3L, "2026-05-02T11:50:00Z", "Fast lunch run with little waiting at crossings.", 3, "5100", 1760, 4, null),
                    log(302L, 3L, "2026-05-05T12:05:00Z", "Good pacing loop; Rathausplatz was busy.", 3, "5200", 1810, 4, null),
                    log(303L, 3L, "2026-05-08T12:15:00Z", "Warm day, took it slightly slower through the inner city.", 4, "5050", 1900, 3,
                            weather(303L, "2026-05-08T12:00:00Z", "23.4", "45", "0", "clear sky", "6.6")),
                    log(304L, 3L, "2026-05-12T11:40:00Z", "Best time so far. Route is now easy to follow.", 3, "5120", 1710, 5, null),
                    log(305L, 3L, "2026-05-15T12:10:00Z", "Short detour near Stadtpark, still a useful workday route.", 3, "5300", 1880, 4,
                            weather(305L, "2026-05-15T12:00:00Z", "21.8", "51", "0", "partly cloudy", "7.3"))
            ),
            4L, List.of()
    ));
    private final AtomicLong nextLogId = new AtomicLong(401L);

    public IntermediateTourLogService(IntermediateTourService tourService) {
        this.tourService = tourService;
        logsByTourId.forEach(tourService::initializeComputedAttributes);
    }

    public Optional<List<TourLogDto>> listLogs(Long tourId) {
        if (tourService.getTour(tourId).isEmpty()) {
            return Optional.empty();
        }

        List<TourLogDto> logs = logsByTourId.getOrDefault(tourId, List.of()).stream()
                .sorted(Comparator.comparing(TourLogDto::performedAt).reversed())
                .toList();

        return Optional.of(logs);
    }

    public Optional<TourLogDto> createLog(Long tourId, CreateTourLogRequest request) {
        Optional<TourDetailDto> tour = tourService.getTour(tourId);
        if (tour.isEmpty()) {
            return Optional.empty();
        }

        Long logId = nextLogId.getAndIncrement();
        Instant now = Instant.now();
        TourLogDto log = new TourLogDto(
                logId,
                tourId,
                request.performedAt(),
                normalizeComment(request.comment()),
                request.difficulty(),
                request.totalDistanceM(),
                request.totalTimeS(),
                request.rating(),
                generatedWeather(logId, tour.get(), request.performedAt(), now),
                now,
                now,
                1L
        );

        logsByTourId.compute(tourId, (ignored, existingLogs) -> {
            List<TourLogDto> updatedLogs = new ArrayList<>(existingLogs == null ? List.of() : existingLogs);
            updatedLogs.add(log);
            return List.copyOf(updatedLogs);
        });
        refreshTourAttributes(tourId);

        return Optional.of(log);
    }

    public Optional<TourLogDto> getLog(Long tourId, Long logId) {
        if (tourService.getTour(tourId).isEmpty()) {
            return Optional.empty();
        }

        return findLog(tourId, logId);
    }

    public Optional<TourLogDto> updateLog(Long tourId, Long logId, UpdateTourLogRequest request) {
        Optional<TourDetailDto> tour = tourService.getTour(tourId);
        if (tour.isEmpty()) {
            return Optional.empty();
        }

        Optional<TourLogDto> existingLog = findLog(tourId, logId);
        if (existingLog.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        TourLogDto updatedLog = new TourLogDto(
                logId,
                tourId,
                request.performedAt(),
                normalizeComment(request.comment()),
                request.difficulty(),
                request.totalDistanceM(),
                request.totalTimeS(),
                request.rating(),
                generatedWeather(logId, tour.get(), request.performedAt(), now),
                existingLog.get().createdAt(),
                now,
                request.version() + 1
        );

        replaceLog(tourId, updatedLog);
        refreshTourAttributes(tourId);
        return Optional.of(updatedLog);
    }

    public boolean deleteLog(Long tourId, Long logId) {
        if (tourService.getTour(tourId).isEmpty()) {
            return false;
        }

        List<TourLogDto> existingLogs = logsByTourId.getOrDefault(tourId, List.of());
        List<TourLogDto> updatedLogs = existingLogs.stream()
                .filter((log) -> !Objects.equals(log.id(), logId))
                .toList();

        if (updatedLogs.size() == existingLogs.size()) {
            return false;
        }

        logsByTourId.put(tourId, List.copyOf(updatedLogs));
        refreshTourAttributes(tourId);
        return true;
    }

    public Optional<TourLogWeatherDto> refreshWeather(Long tourId, Long logId) {
        Optional<TourDetailDto> tour = tourService.getTour(tourId);
        if (tour.isEmpty()) {
            return Optional.empty();
        }

        Optional<TourLogDto> existingLog = findLog(tourId, logId);
        if (existingLog.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        TourLogWeatherDto weather = generatedWeather(logId, tour.get(), existingLog.get().performedAt(), now);
        TourLogDto updatedLog = new TourLogDto(
                existingLog.get().id(),
                existingLog.get().tourId(),
                existingLog.get().performedAt(),
                existingLog.get().comment(),
                existingLog.get().difficulty(),
                existingLog.get().totalDistanceM(),
                existingLog.get().totalTimeS(),
                existingLog.get().rating(),
                weather,
                existingLog.get().createdAt(),
                now,
                existingLog.get().version() + 1
        );

        replaceLog(tourId, updatedLog);
        return Optional.of(weather);
    }

    private Optional<TourLogDto> findLog(Long tourId, Long logId) {
        return logsByTourId.getOrDefault(tourId, List.of()).stream()
                .filter((log) -> Objects.equals(log.id(), logId))
                .findFirst();
    }

    private void replaceLog(Long tourId, TourLogDto updatedLog) {
        List<TourLogDto> updatedLogs = logsByTourId.getOrDefault(tourId, List.of()).stream()
                .map((log) -> Objects.equals(log.id(), updatedLog.id()) ? updatedLog : log)
                .toList();

        logsByTourId.put(tourId, List.copyOf(updatedLogs));
    }

    private void refreshTourAttributes(Long tourId) {
        tourService.refreshComputedAttributes(tourId, logsByTourId.getOrDefault(tourId, List.of()));
    }

    private TourLogWeatherDto generatedWeather(
            Long tourLogId,
            TourDetailDto tour,
            Instant performedAt,
            Instant fetchedAt
    ) {
        Instant observedAt = performedAt.truncatedTo(ChronoUnit.HOURS);
        long hourBucket = Math.floorMod(observedAt.getEpochSecond() / 3600, 24);
        String description = switch ((int) (hourBucket % 4)) {
            case 0 -> "clear sky";
            case 1 -> "partly cloudy";
            case 2 -> "cloudy";
            default -> "light breeze";
        };

        return new TourLogWeatherDto(
                tourLogId,
                "OPEN_METEO",
                "intermediate-generated",
                lookupCoordinate(tour),
                observedAt,
                BigDecimal.valueOf(9.5 + (hourBucket % 10) * 1.1),
                BigDecimal.valueOf(48 + Math.floorMod(hourBucket * 3, 35)),
                BigDecimal.ZERO,
                (int) (hourBucket % 4),
                description,
                BigDecimal.valueOf(6.0 + (hourBucket % 7) * 1.4),
                fetchedAt
        );
    }

    private CoordinateDto lookupCoordinate(TourDetailDto tour) {
        if (tour.route() == null || tour.route().midpointCoordinate() == null) {
            return new CoordinateDto(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        return tour.route().midpointCoordinate();
    }

    private static String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }

        return comment.trim();
    }

    private static TourLogDto log(
            Long id,
            Long tourId,
            String performedAt,
            String comment,
            int difficulty,
            String totalDistanceM,
            int totalTimeS,
            int rating,
            TourLogWeatherDto weather
    ) {
        Instant performed = Instant.parse(performedAt);

        return new TourLogDto(
                id,
                tourId,
                performed,
                comment,
                (short) difficulty,
                new BigDecimal(totalDistanceM),
                totalTimeS,
                (short) rating,
                weather,
                performed.minusSeconds(3600),
                performed.minusSeconds(1800),
                1L
        );
    }

    private static TourLogWeatherDto weather(
            Long tourLogId,
            String observedAt,
            String temperatureC,
            String humidityPercent,
            String precipitationMm,
            String description,
            String windSpeedKmh
    ) {
        Instant observed = Instant.parse(observedAt);

        return new TourLogWeatherDto(
                tourLogId,
                "OPEN_METEO",
                "historical-hourly",
                new CoordinateDto(new BigDecimal("48.2530"), new BigDecimal("16.3801")),
                observed,
                new BigDecimal(temperatureC),
                new BigDecimal(humidityPercent),
                new BigDecimal(precipitationMm),
                0,
                description,
                new BigDecimal(windSpeedKmh),
                observed.plusSeconds(30)
        );
    }
}
