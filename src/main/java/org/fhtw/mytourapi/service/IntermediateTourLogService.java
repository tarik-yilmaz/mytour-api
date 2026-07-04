package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CreateTourLogRequest;
import org.fhtw.mytourapi.dto.ImportedTourLogDto;
import org.fhtw.mytourapi.dto.ImportedWeatherSnapshotDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TourLogWeatherDto;
import org.fhtw.mytourapi.dto.UpdateTourLogRequest;
import org.fhtw.mytourapi.domain.TourEntity;
import org.fhtw.mytourapi.domain.TourLogEntity;
import org.fhtw.mytourapi.mapper.TourPersistenceMapper;
import org.fhtw.mytourapi.repository.TourLogRepository;
import org.fhtw.mytourapi.repository.TourRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateTourLogService.class);

    private final IntermediateTourService tourService;
    private final IntermediateTourSearchIndex tourSearchIndex;
    private final WeatherSnapshotService weatherSnapshotService;
    private final TourRepository tourRepository;
    private final TourLogRepository tourLogRepository;
    private final TourPersistenceMapper persistenceMapper;
    private final boolean persistentStore;
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

    public IntermediateTourLogService(
            IntermediateTourService tourService,
            IntermediateTourSearchIndex tourSearchIndex,
            WeatherSnapshotService weatherSnapshotService
    ) {
        this(
                tourService,
                tourSearchIndex,
                weatherSnapshotService,
                (TourRepository) null,
                (TourLogRepository) null,
                (TourPersistenceMapper) null
        );
    }

    @Autowired
    public IntermediateTourLogService(
            IntermediateTourService tourService,
            IntermediateTourSearchIndex tourSearchIndex,
            WeatherSnapshotService weatherSnapshotService,
            ObjectProvider<TourRepository> tourRepositoryProvider,
            ObjectProvider<TourLogRepository> tourLogRepositoryProvider,
            TourPersistenceMapper persistenceMapper
    ) {
        this(
                tourService,
                tourSearchIndex,
                weatherSnapshotService,
                tourRepositoryProvider.getIfAvailable(),
                tourLogRepositoryProvider.getIfAvailable(),
                persistenceMapper
        );
    }

    IntermediateTourLogService(
            IntermediateTourService tourService,
            IntermediateTourSearchIndex tourSearchIndex,
            WeatherSnapshotService weatherSnapshotService,
            TourRepository tourRepository,
            TourLogRepository tourLogRepository,
            TourPersistenceMapper persistenceMapper
    ) {
        this.tourService = tourService;
        this.tourSearchIndex = tourSearchIndex;
        this.weatherSnapshotService = weatherSnapshotService;
        this.tourRepository = tourRepository;
        this.tourLogRepository = tourLogRepository;
        this.persistenceMapper = persistenceMapper;
        this.persistentStore = tourService.usesPersistentStore()
                && tourRepository != null
                && tourLogRepository != null
                && persistenceMapper != null;
        initializeInMemoryStoreIfNeeded();
    }

    public IntermediateTourLogService(
            IntermediateTourService tourService,
            IntermediateTourSearchIndex tourSearchIndex
    ) {
        this(tourService, tourSearchIndex, WeatherSnapshotService.localFallback());
    }

    @Transactional(readOnly = true)
    public Optional<List<TourLogDto>> listLogs(Long tourId) {
        if (persistentStore) {
            return listPersistedLogs(tourId);
        }

        if (tourService.getTour(tourId).isEmpty()) {
            return Optional.empty();
        }

        List<TourLogDto> logs = logsByTourId.getOrDefault(tourId, List.of()).stream()
                .sorted(Comparator.comparing(TourLogDto::performedAt).reversed())
                .toList();

        return Optional.of(logs);
    }

    @Transactional(readOnly = true)
    public List<TourLogDto> listLogsForExport(Long tourId) {
        if (persistentStore) {
            return listPersistedLogsForExport(tourId);
        }

        return logsByTourId.getOrDefault(tourId, List.of()).stream()
                .sorted(Comparator.comparing(TourLogDto::performedAt).thenComparing(TourLogDto::id))
                .toList();
    }

    @Transactional
    public Optional<TourLogDto> createLog(Long tourId, CreateTourLogRequest request) {
        if (persistentStore) {
            return createPersistedLog(tourId, request);
        }

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
                weatherSnapshotService.snapshotFor(logId, tour.get(), request.performedAt(), now),
                now,
                now,
                1L
        );

        logsByTourId.compute(tourId, (ignored, existingLogs) -> {
            List<TourLogDto> updatedLogs = new ArrayList<>(existingLogs == null ? List.of() : existingLogs);
            updatedLogs.add(log);
            return List.copyOf(updatedLogs);
        });
        refreshDerivedTourState(tourId);
        LOGGER.info(
                "Created intermediate tour log tourId={} logId={} weatherProvider={}",
                tourId,
                logId,
                log.weather() == null ? null : log.weather().provider()
        );

        return Optional.of(log);
    }

    @Transactional
    public Optional<List<TourLogDto>> importLogs(Long tourId, List<ImportedTourLogDto> importedLogs) {
        if (persistentStore) {
            return importPersistedLogs(tourId, importedLogs);
        }

        if (tourService.getTour(tourId).isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        List<TourLogDto> logs = importedLogs.stream()
                .map((importedLog) -> toImportedLog(tourId, importedLog, now))
                .toList();

        logsByTourId.compute(tourId, (ignored, existingLogs) -> {
            List<TourLogDto> updatedLogs = new ArrayList<>(existingLogs == null ? List.of() : existingLogs);
            updatedLogs.addAll(logs);
            return List.copyOf(updatedLogs);
        });
        refreshDerivedTourState(tourId);
        LOGGER.info("Imported intermediate tour logs tourId={} count={}", tourId, logs.size());

        return Optional.of(logs);
    }

    @Transactional(readOnly = true)
    public Optional<TourLogDto> getLog(Long tourId, Long logId) {
        if (persistentStore) {
            return findPersistedLog(tourId, logId)
                    .map(persistenceMapper::toLog);
        }

        if (tourService.getTour(tourId).isEmpty()) {
            return Optional.empty();
        }

        return findLog(tourId, logId);
    }

    @Transactional
    public Optional<TourLogDto> updateLog(Long tourId, Long logId, UpdateTourLogRequest request) {
        if (persistentStore) {
            return updatePersistedLog(tourId, logId, request);
        }

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
                weatherSnapshotService.snapshotFor(logId, tour.get(), request.performedAt(), now),
                existingLog.get().createdAt(),
                now,
                request.version() + 1
        );

        replaceLog(tourId, updatedLog);
        refreshDerivedTourState(tourId);
        LOGGER.info("Updated intermediate tour log tourId={} logId={} version={}", tourId, logId, updatedLog.version());
        return Optional.of(updatedLog);
    }

    @Transactional
    public boolean deleteLog(Long tourId, Long logId) {
        if (persistentStore) {
            return deletePersistedLog(tourId, logId);
        }

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
        refreshDerivedTourState(tourId);
        LOGGER.info("Deleted intermediate tour log tourId={} logId={}", tourId, logId);
        return true;
    }

    @Transactional
    public Optional<TourLogWeatherDto> refreshWeather(Long tourId, Long logId) {
        if (persistentStore) {
            return refreshPersistedWeather(tourId, logId);
        }

        Optional<TourDetailDto> tour = tourService.getTour(tourId);
        if (tour.isEmpty()) {
            return Optional.empty();
        }

        Optional<TourLogDto> existingLog = findLog(tourId, logId);
        if (existingLog.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        TourLogWeatherDto weather = weatherSnapshotService.snapshotFor(logId, tour.get(), existingLog.get().performedAt(), now);
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
        refreshTourSearchIndex(tourId);
        LOGGER.info("Refreshed intermediate tour log weather tourId={} logId={} provider={}", tourId, logId, weather.provider());
        return Optional.of(weather);
    }

    private void initializeInMemoryStoreIfNeeded() {
        if (persistentStore) {
            LOGGER.info("Initialized persisted tour log store");
            return;
        }

        logsByTourId.forEach((tourId, logs) -> {
            tourService.initializeComputedAttributes(tourId, logs);
            tourSearchIndex.replaceLogs(tourId, logs);
        });
        LOGGER.info("Initialized intermediate tour log store tourCount={}", logsByTourId.size());
    }

    private Optional<List<TourLogDto>> listPersistedLogs(Long tourId) {
        Optional<Long> userId = tourService.currentUserIdIfPresent();
        if (userId.isEmpty() || !tourRepository.existsByIdAndUser_Id(tourId, userId.get())) {
            return Optional.empty();
        }

        List<TourLogDto> logs = tourLogRepository
                .findAllByTour_IdAndTour_User_IdOrderByPerformedAtDesc(tourId, userId.get())
                .stream()
                .map(persistenceMapper::toLog)
                .toList();

        return Optional.of(logs);
    }

    private List<TourLogDto> listPersistedLogsForExport(Long tourId) {
        return tourService.currentUserIdIfPresent()
                .map((userId) -> tourLogRepository
                        .findAllByTour_IdAndTour_User_IdOrderByPerformedAtDesc(tourId, userId).stream()
                        .map(persistenceMapper::toLog)
                        .sorted(Comparator.comparing(TourLogDto::performedAt).thenComparing(TourLogDto::id))
                        .toList())
                .orElseGet(List::of);
    }

    private Optional<TourLogDto> createPersistedLog(Long tourId, CreateTourLogRequest request) {
        Optional<TourEntity> tour = findOwnedTour(tourId);
        if (tour.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        TourLogEntity log = new TourLogEntity();
        log.setTour(tour.get());
        applyLogFields(log, request.performedAt(), request.comment(), request.difficulty(), request.totalDistanceM(),
                request.totalTimeS(), request.rating());

        TourLogEntity savedLog = tourLogRepository.saveAndFlush(log);
        TourLogWeatherDto weather = weatherSnapshotService.snapshotFor(
                savedLog.getId(),
                persistenceMapper.toDetail(tour.get()),
                request.performedAt(),
                now
        );
        persistenceMapper.applyWeather(savedLog, weather);
        savedLog = tourLogRepository.saveAndFlush(savedLog);

        refreshPersistedDerivedTourState(tourId);
        LOGGER.info(
                "Created persisted tour log tourId={} logId={} weatherProvider={}",
                tourId,
                savedLog.getId(),
                weather.provider()
        );
        return Optional.of(persistenceMapper.toLog(savedLog));
    }

    private Optional<List<TourLogDto>> importPersistedLogs(Long tourId, List<ImportedTourLogDto> importedLogs) {
        Optional<TourEntity> tour = findOwnedTour(tourId);
        if (tour.isEmpty()) {
            return Optional.empty();
        }

        List<TourLogDto> logs = importedLogs.stream()
                .map((importedLog) -> {
                    CreateTourLogRequest request = importedLog.log();
                    TourLogEntity log = new TourLogEntity();
                    log.setTour(tour.get());
                    applyLogFields(log, request.performedAt(), request.comment(), request.difficulty(),
                            request.totalDistanceM(), request.totalTimeS(), request.rating());

                    TourLogEntity savedLog = tourLogRepository.saveAndFlush(log);
                    persistenceMapper.applyImportedWeather(savedLog, importedLog.weather());
                    savedLog = tourLogRepository.saveAndFlush(savedLog);
                    return persistenceMapper.toLog(savedLog);
                })
                .toList();

        refreshPersistedDerivedTourState(tourId);
        LOGGER.info("Imported persisted tour logs tourId={} count={}", tourId, logs.size());
        return Optional.of(logs);
    }

    private Optional<TourLogEntity> findPersistedLog(Long tourId, Long logId) {
        return tourService.currentUserIdIfPresent()
                .flatMap((userId) -> tourLogRepository.findByIdAndTour_IdAndTour_User_Id(logId, tourId, userId));
    }

    private Optional<TourLogDto> updatePersistedLog(Long tourId, Long logId, UpdateTourLogRequest request) {
        return findPersistedLog(tourId, logId)
                .map((log) -> {
                    Instant now = Instant.now();
                    applyLogFields(log, request.performedAt(), request.comment(), request.difficulty(),
                            request.totalDistanceM(), request.totalTimeS(), request.rating());

                    TourLogWeatherDto weather = weatherSnapshotService.snapshotFor(
                            logId,
                            persistenceMapper.toDetail(log.getTour()),
                            request.performedAt(),
                            now
                    );
                    persistenceMapper.applyWeather(log, weather);
                    TourLogEntity savedLog = tourLogRepository.saveAndFlush(log);
                    refreshPersistedDerivedTourState(tourId);

                    LOGGER.info("Updated persisted tour log tourId={} logId={} version={}",
                            tourId, logId, savedLog.getVersion());
                    return persistenceMapper.toLog(savedLog);
                });
    }

    private boolean deletePersistedLog(Long tourId, Long logId) {
        Optional<TourLogEntity> log = findPersistedLog(tourId, logId);
        if (log.isEmpty()) {
            return false;
        }

        tourLogRepository.delete(log.get());
        tourLogRepository.flush();
        refreshPersistedDerivedTourState(tourId);
        LOGGER.info("Deleted persisted tour log tourId={} logId={}", tourId, logId);
        return true;
    }

    private Optional<TourLogWeatherDto> refreshPersistedWeather(Long tourId, Long logId) {
        return findPersistedLog(tourId, logId)
                .map((log) -> {
                    Instant now = Instant.now();
                    TourLogWeatherDto weather = weatherSnapshotService.snapshotFor(
                            logId,
                            persistenceMapper.toDetail(log.getTour()),
                            log.getPerformedAt(),
                            now
                    );
                    persistenceMapper.applyWeather(log, weather);
                    tourLogRepository.saveAndFlush(log);
                    refreshPersistedTourSearchIndex(tourId);

                    LOGGER.info(
                            "Refreshed persisted tour log weather tourId={} logId={} provider={}",
                            tourId,
                            logId,
                            weather.provider()
                    );
                    return weather;
                });
    }

    private Optional<TourEntity> findOwnedTour(Long tourId) {
        return tourService.currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId));
    }

    private void applyLogFields(
            TourLogEntity log,
            Instant performedAt,
            String comment,
            Short difficulty,
            BigDecimal totalDistanceM,
            Integer totalTimeS,
            Short rating
    ) {
        log.setPerformedAt(performedAt);
        log.setComment(normalizeComment(comment));
        log.setDifficulty(difficulty);
        log.setTotalDistanceM(totalDistanceM);
        log.setTotalTimeS(totalTimeS);
        log.setRating(rating);
    }

    private void refreshPersistedDerivedTourState(Long tourId) {
        List<TourLogDto> logs = persistedLogsForTour(tourId);
        tourService.refreshComputedAttributes(tourId, logs);
        tourSearchIndex.replaceLogs(tourId, logs);
    }

    private void refreshPersistedTourSearchIndex(Long tourId) {
        tourSearchIndex.replaceLogs(tourId, persistedLogsForTour(tourId));
    }

    private List<TourLogDto> persistedLogsForTour(Long tourId) {
        return tourService.currentUserIdIfPresent()
                .map((userId) -> tourLogRepository
                        .findAllByTour_IdAndTour_User_IdOrderByPerformedAtDesc(tourId, userId).stream()
                        .map(persistenceMapper::toLog)
                        .toList())
                .orElseGet(List::of);
    }

    private TourLogDto toImportedLog(Long tourId, ImportedTourLogDto importedLog, Instant importedAt) {
        Long logId = nextLogId.getAndIncrement();
        CreateTourLogRequest log = importedLog.log();

        return new TourLogDto(
                logId,
                tourId,
                log.performedAt(),
                normalizeComment(log.comment()),
                log.difficulty(),
                log.totalDistanceM(),
                log.totalTimeS(),
                log.rating(),
                toImportedWeather(logId, importedLog.weather(), importedAt),
                importedAt,
                importedAt,
                1L
        );
    }

    private TourLogWeatherDto toImportedWeather(
            Long logId,
            ImportedWeatherSnapshotDto weather,
            Instant importedAt
    ) {
        if (weather == null) {
            return null;
        }

        return new TourLogWeatherDto(
                logId,
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
                weather.fetchedAt() == null ? importedAt : weather.fetchedAt()
        );
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

    private void refreshDerivedTourState(Long tourId) {
        List<TourLogDto> logs = logsByTourId.getOrDefault(tourId, List.of());
        tourService.refreshComputedAttributes(tourId, logs);
        tourSearchIndex.replaceLogs(tourId, logs);
    }

    private void refreshTourSearchIndex(Long tourId) {
        tourSearchIndex.replaceLogs(tourId, logsByTourId.getOrDefault(tourId, List.of()));
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
