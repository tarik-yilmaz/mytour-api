package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.dto.CreateTourRequest;
import org.fhtw.mytourapi.dto.ImportedTourDto;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TourRouteDto;
import org.fhtw.mytourapi.dto.TourSearchResponse;
import org.fhtw.mytourapi.dto.TourSummaryDto;
import org.fhtw.mytourapi.dto.TourSuggestionDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.fhtw.mytourapi.dto.UpdateTourRequest;
import org.fhtw.mytourapi.domain.TourEntity;
import org.fhtw.mytourapi.domain.UserEntity;
import org.fhtw.mytourapi.exception.FileStorageException;
import org.fhtw.mytourapi.mapper.TourPersistenceMapper;
import org.fhtw.mytourapi.repository.TourRepository;
import org.fhtw.mytourapi.repository.UserRepository;
import org.fhtw.mytourapi.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IntermediateTourService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateTourService.class);
    private static final Long INTERMEDIATE_USER_ID = 1L;

    private final Map<Long, TourDetailDto> toursById = new ConcurrentHashMap<>(Map.of(
            1L, new TourDetailDto(
                    1L,
                    1L,
                    "Danube Island Evening Ride",
                    "Easy after-work cycling route along the Danube with wide paths and a calm finish near the water.",
                    "Wien Praterstern",
                    "Donauinsel Nord",
                    TransportType.BIKE,
                    "Europe/Vienna",
                    meters("18200"),
                    4200,
                    new CoverImageDto("intermediate/danube-island.jpg", "danube-island.jpg", "image/jpeg", 264000L),
                    new TourRouteDto(
                            "OPENROUTESERVICE",
                            "cycling-regular",
                            coordinate("48.2189", "16.3927"),
                            coordinate("48.2872", "16.3674"),
                            coordinate("48.2530", "16.3801"),
                            null,
                            Instant.parse("2026-05-10T17:30:00Z")
                    ),
                    new ComputedTourAttributesDto(
                            3,
                            68,
                            PopularityCategory.POPULAR,
                            "popular",
                            88,
                            ChildFriendlinessCategory.FAMILY_FRIENDLY,
                            "family friendly"
                    ),
                    Instant.parse("2026-04-02T08:15:00Z"),
                    Instant.parse("2026-05-10T17:30:00Z"),
                    1L
            ),
            2L, new TourDetailDto(
                    2L,
                    1L,
                    "Kahlenberg Sunrise Hike",
                    "A compact morning hike from Nussdorf up to Kahlenberg with a steady climb and a clear city view.",
                    "Nussdorf",
                    "Kahlenberg",
                    TransportType.HIKE,
                    "Europe/Vienna",
                    meters("7600"),
                    8100,
                    null,
                    new TourRouteDto(
                            "OPENROUTESERVICE",
                            "foot-hiking",
                            coordinate("48.2601", "16.3688"),
                            coordinate("48.2767", "16.3339"),
                            coordinate("48.2684", "16.3512"),
                            null,
                            Instant.parse("2026-05-03T06:25:00Z")
                    ),
                    new ComputedTourAttributesDto(
                            1,
                            24,
                            PopularityCategory.RARELY_USED,
                            "rarely used",
                            36,
                            ChildFriendlinessCategory.CHALLENGING_ROUTE,
                            "challenging route"
                    ),
                    Instant.parse("2026-03-19T06:00:00Z"),
                    Instant.parse("2026-05-03T06:25:00Z"),
                    1L
            ),
            3L, new TourDetailDto(
                    3L,
                    1L,
                    "Ringstrasse Lunch Run",
                    "Short urban running loop for lunch breaks, with predictable streets and several easy exit points.",
                    "Stadtpark",
                    "Rathausplatz",
                    TransportType.RUNNING,
                    "Europe/Vienna",
                    meters("5100"),
                    1800,
                    null,
                    new TourRouteDto(
                            "OPENROUTESERVICE",
                            "foot-walking",
                            coordinate("48.2042", "16.3802"),
                            coordinate("48.2109", "16.3576"),
                            coordinate("48.2075", "16.3689"),
                            null,
                            Instant.parse("2026-05-15T12:20:00Z")
                    ),
                    new ComputedTourAttributesDto(
                            5,
                            92,
                            PopularityCategory.VERY_POPULAR,
                            "very popular",
                            18,
                            ChildFriendlinessCategory.ADULT_ORIENTED,
                            "adult oriented"
                    ),
                    Instant.parse("2026-02-11T11:45:00Z"),
                    Instant.parse("2026-05-15T12:20:00Z"),
                    1L
            ),
            4L, new TourDetailDto(
                    4L,
                    1L,
                    "Salzkammergut Weekend",
                    "Vacation tour draft from Bad Ischl to Hallstatt for a relaxed weekend route with photo stops.",
                    "Bad Ischl",
                    "Hallstatt",
                    TransportType.VACATION,
                    "Europe/Vienna",
                    meters("22400"),
                    14400,
                    null,
                    new TourRouteDto(
                            "OPENROUTESERVICE",
                            "driving-car",
                            coordinate("47.7111", "13.6236"),
                            coordinate("47.5622", "13.6493"),
                            coordinate("47.6367", "13.6365"),
                            null,
                            Instant.parse("2026-05-01T09:00:00Z")
                    ),
                    new ComputedTourAttributesDto(
                            0,
                            0,
                            PopularityCategory.NEW,
                            "new",
                            0,
                            ChildFriendlinessCategory.UNKNOWN,
                            "unknown"
                    ),
                    Instant.parse("2026-05-01T09:00:00Z"),
                    Instant.parse("2026-05-01T09:00:00Z"),
                    1L
            )
    ));
    private final AtomicLong nextTourId = new AtomicLong(5L);

    private final RouteCalculationService routeCalculationService;
    private final CoverImageStorageService coverImageStorageService;
    private final TourAttributeCalculator tourAttributeCalculator;
    private final IntermediateTourSearchIndex tourSearchIndex;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourPersistenceMapper persistenceMapper;
    private final CurrentUserService currentUserService;
    private final boolean persistentStore;

    public IntermediateTourService(
            RouteCalculationService routeCalculationService,
            CoverImageStorageService coverImageStorageService,
            TourAttributeCalculator tourAttributeCalculator,
            IntermediateTourSearchIndex tourSearchIndex
    ) {
        this(
                routeCalculationService,
                coverImageStorageService,
                tourAttributeCalculator,
                tourSearchIndex,
                (TourRepository) null,
                (UserRepository) null,
                (TourPersistenceMapper) null,
                (CurrentUserService) null
        );
    }

    @Autowired
    public IntermediateTourService(
            RouteCalculationService routeCalculationService,
            CoverImageStorageService coverImageStorageService,
            TourAttributeCalculator tourAttributeCalculator,
            IntermediateTourSearchIndex tourSearchIndex,
            ObjectProvider<TourRepository> tourRepositoryProvider,
            ObjectProvider<UserRepository> userRepositoryProvider,
            TourPersistenceMapper persistenceMapper,
            ObjectProvider<CurrentUserService> currentUserServiceProvider
    ) {
        this(
                routeCalculationService,
                coverImageStorageService,
                tourAttributeCalculator,
                tourSearchIndex,
                tourRepositoryProvider.getIfAvailable(),
                userRepositoryProvider.getIfAvailable(),
                persistenceMapper,
                currentUserServiceProvider.getIfAvailable()
        );
    }

    IntermediateTourService(
            RouteCalculationService routeCalculationService,
            CoverImageStorageService coverImageStorageService,
            TourAttributeCalculator tourAttributeCalculator,
            IntermediateTourSearchIndex tourSearchIndex,
            TourRepository tourRepository,
            UserRepository userRepository,
            TourPersistenceMapper persistenceMapper,
            CurrentUserService currentUserService
    ) {
        this.routeCalculationService = routeCalculationService;
        this.coverImageStorageService = coverImageStorageService;
        this.tourAttributeCalculator = tourAttributeCalculator;
        this.tourSearchIndex = tourSearchIndex;
        this.tourRepository = tourRepository;
        this.userRepository = userRepository;
        this.persistenceMapper = persistenceMapper;
        this.currentUserService = currentUserService;
        this.persistentStore = tourRepository != null
                && userRepository != null
                && persistenceMapper != null
                && currentUserService != null;
    }

    @Transactional(readOnly = true)
    public TourSearchResponse searchTours(
            String query,
            TransportType transportType,
            PopularityCategory popularity,
            ChildFriendlinessCategory childFriendliness,
            Short ratingMin
    ) {
        if (persistentStore) {
            return searchPersistedTours(query, transportType, popularity, childFriendliness, ratingMin);
        }

        List<TourSummaryDto> tours = toursById.values().stream()
                .filter((tour) -> transportType == null || tour.transportType() == transportType)
                .filter((tour) -> popularity == null || tour.computedAttributes().popularityCategory() == popularity)
                .filter((tour) -> childFriendliness == null
                        || tour.computedAttributes().childFriendlinessCategory() == childFriendliness)
                .filter((tour) -> tourSearchIndex.matches(tour, query, ratingMin))
                .map(this::toSummary)
                .sorted(Comparator.comparing(TourSummaryDto::id))
                .toList();

        LOGGER.debug(
                "Searched intermediate tours resultCount={} hasQuery={} transportType={} popularity={} childFriendliness={} ratingMin={}",
                tours.size(),
                query != null && !query.isBlank(),
                transportType,
                popularity,
                childFriendliness,
                ratingMin
        );
        return new TourSearchResponse(tours, tours.size());
    }

    @Transactional(readOnly = true)
    public List<TourSuggestionDto> suggestTours(String query, int limit) {
        String trimmedQuery = query == null ? "" : query.trim();
        if (trimmedQuery.length() < 2) {
            return List.of();
        }

        if (persistentStore) {
            return suggestPersistedTours(trimmedQuery, limit);
        }

        return toursById.values().stream()
                .filter((tour) -> tourSearchIndex.matches(tour, trimmedQuery, null))
                .sorted(Comparator.comparing(TourDetailDto::name, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .map(this::toSuggestion)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TourDetailDto> getTour(Long tourId) {
        if (persistentStore) {
            return currentUserIdIfPresent()
                    .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId))
                    .map(persistenceMapper::toDetail);
        }

        return Optional.ofNullable(toursById.get(tourId));
    }

    @Transactional(readOnly = true)
    public List<TourDetailDto> listToursForExport() {
        if (persistentStore) {
            return currentUserIdIfPresent()
                    .map((userId) -> tourRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId).stream()
                    .sorted(Comparator.comparing(TourEntity::getId))
                    .map(persistenceMapper::toDetail)
                    .toList())
                    .orElseGet(List::of);
        }

        return toursById.values().stream()
                .sorted(Comparator.comparing(TourDetailDto::id))
                .toList();
    }

    @Transactional
    public TourDetailDto createTour(CreateTourRequest request) {
        if (persistentStore) {
            return createPersistedTour(request);
        }

        Long tourId = nextTourId.getAndIncrement();
        Instant now = Instant.now();
        TourDetailDto tour = fromRequest(
                tourId,
                request.name(),
                request.description(),
                request.startLocation(),
                request.endLocation(),
                request.transportType(),
                request.timezoneId(),
                request.startCoordinate(),
                request.endCoordinate(),
                null,
                defaultComputedAttributes(),
                now,
                now,
                1L
        );

        toursById.put(tourId, tour);
        LOGGER.info(
                "Created intermediate tour tourId={} transportType={} routeSource={}",
                tourId,
                tour.transportType(),
                tour.route().routeSource()
        );
        return tour;
    }

    @Transactional
    public TourDetailDto importTour(ImportedTourDto importedTour) {
        if (persistentStore) {
            return importPersistedTour(importedTour);
        }

        Long tourId = nextTourId.getAndIncrement();
        Instant now = Instant.now();
        CreateTourRequest request = importedTour.tour();
        TourDetailDto tour = new TourDetailDto(
                tourId,
                INTERMEDIATE_USER_ID,
                request.name(),
                request.description(),
                request.startLocation(),
                request.endLocation(),
                request.transportType(),
                request.timezoneId(),
                importedTour.plannedDistanceM(),
                importedTour.estimatedDurationS(),
                importedTour.coverImage(),
                importedTour.route(),
                defaultComputedAttributes(),
                now,
                now,
                1L
        );

        toursById.put(tourId, tour);
        LOGGER.info(
                "Imported intermediate tour tourId={} routeSource={}",
                tourId,
                tour.route() == null ? null : tour.route().routeSource()
        );
        return tour;
    }

    @Transactional
    public Optional<TourDetailDto> updateTour(Long tourId, UpdateTourRequest request) {
        if (persistentStore) {
            return updatePersistedTour(tourId, request);
        }

        TourDetailDto existingTour = toursById.get(tourId);
        if (existingTour == null) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        TourDetailDto updatedTour = fromRequest(
                tourId,
                request.name(),
                request.description(),
                request.startLocation(),
                request.endLocation(),
                request.transportType(),
                request.timezoneId(),
                request.startCoordinate(),
                request.endCoordinate(),
                existingTour.coverImage(),
                existingTour.computedAttributes(),
                existingTour.createdAt(),
                now,
                request.version() + 1
        );

        toursById.put(tourId, updatedTour);
        LOGGER.info("Updated intermediate tour tourId={} version={}", tourId, updatedTour.version());
        return Optional.of(updatedTour);
    }

    @Transactional
    public boolean deleteTour(Long tourId) {
        if (persistentStore) {
            return deletePersistedTour(tourId);
        }

        TourDetailDto existingTour = toursById.get(tourId);
        if (existingTour == null) {
            return false;
        }

        deleteStoredCoverImage(existingTour.coverImage());
        tourSearchIndex.removeTour(tourId);
        toursById.remove(tourId);
        LOGGER.info("Deleted intermediate tour tourId={}", tourId);
        return true;
    }

    @Transactional
    public Optional<TourRouteDto> refreshRoute(Long tourId) {
        if (persistentStore) {
            return refreshPersistedRoute(tourId);
        }

        TourDetailDto existingTour = toursById.get(tourId);
        if (existingTour == null || existingTour.route() == null) {
            return Optional.empty();
        }

        TourRouteDto existingRoute = existingTour.route();
        if (existingRoute.startCoordinate() == null || existingRoute.endCoordinate() == null) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        CalculatedRoute calculatedRoute = routeCalculationService.calculateRoute(
                existingTour.transportType(),
                existingRoute.startCoordinate(),
                existingRoute.endCoordinate(),
                now
        );
        TourDetailDto updatedTour = withCalculatedRoute(existingTour, calculatedRoute, now);

        toursById.put(tourId, updatedTour);
        LOGGER.info(
                "Refreshed intermediate tour route tourId={} routeSource={}",
                tourId,
                calculatedRoute.route().routeSource()
        );
        return Optional.of(calculatedRoute.route());
    }

    @Transactional
    public Optional<CoverImageDto> uploadCoverImage(Long tourId, MultipartFile file) {
        if (persistentStore) {
            return uploadPersistedCoverImage(tourId, file);
        }

        TourDetailDto existingTour = toursById.get(tourId);
        if (existingTour == null) {
            return Optional.empty();
        }

        CoverImageDto previousCoverImage = existingTour.coverImage();
        CoverImageDto storedCoverImage = coverImageStorageService.store(file);
        Instant now = Instant.now();
        TourDetailDto updatedTour = withCoverImage(existingTour, storedCoverImage, now);

        toursById.put(tourId, updatedTour);
        deletePreviousCoverImage(tourId, previousCoverImage);
        LOGGER.info(
                "Updated intermediate tour cover image tourId={} contentType={} sizeBytes={}",
                tourId,
                storedCoverImage.contentType(),
                storedCoverImage.sizeBytes()
        );
        return Optional.of(storedCoverImage);
    }

    @Transactional
    public boolean deleteCoverImage(Long tourId) {
        if (persistentStore) {
            return deletePersistedCoverImage(tourId);
        }

        TourDetailDto existingTour = toursById.get(tourId);
        if (existingTour == null) {
            return false;
        }

        if (existingTour.coverImage() == null) {
            return true;
        }

        deleteStoredCoverImage(existingTour.coverImage());
        toursById.put(tourId, withCoverImage(existingTour, null, Instant.now()));
        LOGGER.info("Deleted intermediate tour cover image tourId={}", tourId);
        return true;
    }

    @Transactional
    public Optional<TourDetailDto> initializeComputedAttributes(Long tourId, List<TourLogDto> logs) {
        if (persistentStore) {
            return replacePersistedComputedAttributes(tourId, logs);
        }

        TourDetailDto existingTour = toursById.get(tourId);
        if (existingTour == null) {
            return Optional.empty();
        }

        return replaceComputedAttributes(tourId, logs, existingTour.updatedAt(), existingTour.version());
    }

    @Transactional
    public Optional<TourDetailDto> refreshComputedAttributes(Long tourId, List<TourLogDto> logs) {
        if (persistentStore) {
            return replacePersistedComputedAttributes(tourId, logs);
        }

        TourDetailDto existingTour = toursById.get(tourId);
        if (existingTour == null) {
            return Optional.empty();
        }

        return replaceComputedAttributes(tourId, logs, Instant.now(), nextVersion(existingTour.version()));
    }

    boolean usesPersistentStore() {
        return persistentStore;
    }

    Optional<Long> currentUserIdIfPresent() {
        if (!persistentStore) {
            return Optional.of(INTERMEDIATE_USER_ID);
        }

        return currentUserService.currentUserIdIfAuthenticated();
    }

    Long createOrGetCurrentUserId() {
        if (!persistentStore) {
            return INTERMEDIATE_USER_ID;
        }

        return currentUserService.currentUserId();
    }

    private TourSearchResponse searchPersistedTours(
            String query,
            TransportType transportType,
            PopularityCategory popularity,
            ChildFriendlinessCategory childFriendliness,
            Short ratingMin
    ) {
        Optional<Long> userId = currentUserIdIfPresent();
        if (userId.isEmpty()) {
            return new TourSearchResponse(List.of(), 0);
        }

        List<TourSummaryDto> tours = tourRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId.get()).stream()
                .filter((tour) -> transportType == null || tour.getTransportType().name().equals(transportType.name()))
                .filter((tour) -> popularity == null || tour.getPopularityCategory().name().equals(popularity.name()))
                .filter((tour) -> childFriendliness == null
                        || tour.getChildFriendlinessCategory().name().equals(childFriendliness.name()))
                .filter((tour) -> persistedTourMatches(tour, query, ratingMin))
                .map(persistenceMapper::toSummary)
                .sorted(Comparator.comparing(TourSummaryDto::id))
                .toList();

        LOGGER.debug(
                "Searched persisted tours resultCount={} hasQuery={} transportType={} popularity={} childFriendliness={} ratingMin={}",
                tours.size(),
                query != null && !query.isBlank(),
                transportType,
                popularity,
                childFriendliness,
                ratingMin
        );
        return new TourSearchResponse(tours, tours.size());
    }

    private List<TourSuggestionDto> suggestPersistedTours(String query, int limit) {
        Optional<Long> userId = currentUserIdIfPresent();
        if (userId.isEmpty()) {
            return List.of();
        }

        return tourRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId.get()).stream()
                .filter((tour) -> persistedTourMatches(tour, query, null))
                .map(persistenceMapper::toDetail)
                .sorted(Comparator.comparing(TourDetailDto::name, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .map(this::toSuggestion)
                .toList();
    }

    private boolean persistedTourMatches(TourEntity tour, String query, Short ratingMin) {
        List<TourLogDto> logs = tour.getLogs().stream()
                .map(persistenceMapper::toLog)
                .toList();
        TourDetailDto detail = persistenceMapper.toDetail(tour);

        tourSearchIndex.replaceLogs(tour.getId(), logs);
        return tourSearchIndex.matches(detail, query, ratingMin);
    }

    private TourSuggestionDto toSuggestion(TourDetailDto tour) {
        String route = tour.startLocation() + " -> " + tour.endLocation();
        return new TourSuggestionDto(
                tour.id(),
                tour.name(),
                route,
                route
        );
    }

    private TourDetailDto createPersistedTour(CreateTourRequest request) {
        Instant now = Instant.now();
        CalculatedRoute calculatedRoute = routeCalculationService.calculateRoute(
                request.transportType(),
                request.startCoordinate(),
                request.endCoordinate(),
                now
        );

        TourEntity tour = new TourEntity();
        tour.setUser(currentUser());
        applyTourFields(
                tour,
                request.name(),
                request.description(),
                request.startLocation(),
                request.endLocation(),
                request.transportType(),
                request.timezoneId()
        );
        tour.setPlannedDistanceM(calculatedRoute.distanceM());
        tour.setEstimatedDurationS(calculatedRoute.durationS());
        persistenceMapper.applyComputedAttributes(tour, defaultComputedAttributes());
        persistenceMapper.applyRoute(tour, calculatedRoute.route());

        TourEntity savedTour = tourRepository.saveAndFlush(tour);
        LOGGER.info(
                "Created persisted tour tourId={} transportType={} routeSource={}",
                savedTour.getId(),
                savedTour.getTransportType(),
                savedTour.getRoute().getRouteSource()
        );
        return persistenceMapper.toDetail(savedTour);
    }

    private TourDetailDto importPersistedTour(ImportedTourDto importedTour) {
        CreateTourRequest request = importedTour.tour();
        TourEntity tour = new TourEntity();
        tour.setUser(currentUser());
        applyTourFields(
                tour,
                request.name(),
                request.description(),
                request.startLocation(),
                request.endLocation(),
                request.transportType(),
                request.timezoneId()
        );
        tour.setPlannedDistanceM(importedTour.plannedDistanceM());
        tour.setEstimatedDurationS(importedTour.estimatedDurationS());
        persistenceMapper.applyCoverImage(tour, importedTour.coverImage());
        persistenceMapper.applyComputedAttributes(tour, defaultComputedAttributes());
        persistenceMapper.applyRoute(tour, importedTour.route());

        TourEntity savedTour = tourRepository.saveAndFlush(tour);
        LOGGER.info(
                "Imported persisted tour tourId={} routeSource={}",
                savedTour.getId(),
                savedTour.getRoute() == null ? null : savedTour.getRoute().getRouteSource()
        );
        return persistenceMapper.toDetail(savedTour);
    }

    private Optional<TourDetailDto> updatePersistedTour(Long tourId, UpdateTourRequest request) {
        return currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId))
                .map((tour) -> {
                    Instant now = Instant.now();
                    CalculatedRoute calculatedRoute = routeCalculationService.calculateRoute(
                            request.transportType(),
                            request.startCoordinate(),
                            request.endCoordinate(),
                            now
                    );

                    applyTourFields(
                            tour,
                            request.name(),
                            request.description(),
                            request.startLocation(),
                            request.endLocation(),
                            request.transportType(),
                            request.timezoneId()
                    );
                    tour.setPlannedDistanceM(calculatedRoute.distanceM());
                    tour.setEstimatedDurationS(calculatedRoute.durationS());
                    persistenceMapper.applyRoute(tour, calculatedRoute.route());

                    TourEntity savedTour = tourRepository.saveAndFlush(tour);
                    LOGGER.info("Updated persisted tour tourId={} version={}", tourId, savedTour.getVersion());
                    return persistenceMapper.toDetail(savedTour);
                });
    }

    private boolean deletePersistedTour(Long tourId) {
        Optional<TourEntity> tour = currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId));
        if (tour.isEmpty()) {
            return false;
        }

        deleteStoredCoverImage(persistenceMapper.toCoverImage(tour.get()));
        tourSearchIndex.removeTour(tourId);
        tourRepository.delete(tour.get());
        tourRepository.flush();
        LOGGER.info("Deleted persisted tour tourId={}", tourId);
        return true;
    }

    private Optional<TourRouteDto> refreshPersistedRoute(Long tourId) {
        return currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId))
                .flatMap((tour) -> {
                    TourRouteDto existingRoute = persistenceMapper.toRoute(tour.getRoute());
                    if (existingRoute == null
                            || existingRoute.startCoordinate() == null
                            || existingRoute.endCoordinate() == null) {
                        return Optional.empty();
                    }

                    Instant now = Instant.now();
                    CalculatedRoute calculatedRoute = routeCalculationService.calculateRoute(
                            persistenceMapper.toDetail(tour).transportType(),
                            existingRoute.startCoordinate(),
                            existingRoute.endCoordinate(),
                            now
                    );
                    tour.setPlannedDistanceM(calculatedRoute.distanceM());
                    tour.setEstimatedDurationS(calculatedRoute.durationS());
                    persistenceMapper.applyRoute(tour, calculatedRoute.route());
                    TourEntity savedTour = tourRepository.saveAndFlush(tour);

                    LOGGER.info(
                            "Refreshed persisted tour route tourId={} routeSource={}",
                            tourId,
                            savedTour.getRoute().getRouteSource()
                    );
                    return Optional.of(persistenceMapper.toRoute(savedTour.getRoute()));
                });
    }

    private Optional<CoverImageDto> uploadPersistedCoverImage(Long tourId, MultipartFile file) {
        return currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId))
                .map((tour) -> {
                    CoverImageDto previousCoverImage = persistenceMapper.toCoverImage(tour);
                    CoverImageDto storedCoverImage = coverImageStorageService.store(file);
                    persistenceMapper.applyCoverImage(tour, storedCoverImage);
                    tourRepository.saveAndFlush(tour);
                    deletePreviousCoverImage(tourId, previousCoverImage);
                    LOGGER.info(
                            "Updated persisted tour cover image tourId={} contentType={} sizeBytes={}",
                            tourId,
                            storedCoverImage.contentType(),
                            storedCoverImage.sizeBytes()
                    );
                    return storedCoverImage;
                });
    }

    private boolean deletePersistedCoverImage(Long tourId) {
        Optional<TourEntity> tour = currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId));
        if (tour.isEmpty()) {
            return false;
        }

        CoverImageDto coverImage = persistenceMapper.toCoverImage(tour.get());
        if (coverImage == null) {
            return true;
        }

        deleteStoredCoverImage(coverImage);
        persistenceMapper.applyCoverImage(tour.get(), null);
        tourRepository.saveAndFlush(tour.get());
        LOGGER.info("Deleted persisted tour cover image tourId={}", tourId);
        return true;
    }

    private Optional<TourDetailDto> replacePersistedComputedAttributes(Long tourId, List<TourLogDto> logs) {
        return currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId))
                .map((tour) -> {
                    ComputedTourAttributesDto computedAttributes = tourAttributeCalculator.calculate(logs);
                    persistenceMapper.applyComputedAttributes(tour, computedAttributes);
                    TourEntity savedTour = tourRepository.saveAndFlush(tour);
                    LOGGER.debug(
                            "Refreshed computed attributes for persisted tour tourId={} logCount={}",
                            tourId,
                            logs.size()
                    );
                    return persistenceMapper.toDetail(savedTour);
                });
    }

    private void applyTourFields(
            TourEntity tour,
            String name,
            String description,
            String startLocation,
            String endLocation,
            TransportType transportType,
            String timezoneId
    ) {
        tour.setName(name);
        tour.setDescription(description);
        tour.setStartLocation(startLocation);
        tour.setEndLocation(endLocation);
        tour.setTransportType(persistenceMapper.toDomainTransportType(transportType));
        tour.setTimezoneId(timezoneId);
    }

    private UserEntity currentUser() {
        return currentUserService.currentUser();
    }

    private TourSummaryDto toSummary(TourDetailDto tour) {
        return new TourSummaryDto(
                tour.id(),
                tour.userId(),
                tour.name(),
                tour.startLocation(),
                tour.endLocation(),
                tour.transportType(),
                tour.timezoneId(),
                tour.plannedDistanceM(),
                tour.estimatedDurationS(),
                tour.coverImage(),
                tour.computedAttributes(),
                tour.createdAt(),
                tour.updatedAt()
        );
    }

    private TourDetailDto fromRequest(
            Long tourId,
            String name,
            String description,
            String startLocation,
            String endLocation,
            TransportType transportType,
            String timezoneId,
            CoordinateDto startCoordinate,
            CoordinateDto endCoordinate,
            CoverImageDto coverImage,
            ComputedTourAttributesDto computedAttributes,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        CalculatedRoute calculatedRoute = routeCalculationService.calculateRoute(
                transportType,
                startCoordinate,
                endCoordinate,
                updatedAt
        );

        return new TourDetailDto(
                tourId,
                INTERMEDIATE_USER_ID,
                name,
                description,
                startLocation,
                endLocation,
                transportType,
                timezoneId,
                calculatedRoute.distanceM(),
                calculatedRoute.durationS(),
                coverImage,
                calculatedRoute.route(),
                computedAttributes,
                createdAt,
                updatedAt,
                version
        );
    }

    private TourDetailDto withCalculatedRoute(
            TourDetailDto existingTour,
            CalculatedRoute calculatedRoute,
            Instant updatedAt
    ) {
        return new TourDetailDto(
                existingTour.id(),
                existingTour.userId(),
                existingTour.name(),
                existingTour.description(),
                existingTour.startLocation(),
                existingTour.endLocation(),
                existingTour.transportType(),
                existingTour.timezoneId(),
                calculatedRoute.distanceM(),
                calculatedRoute.durationS(),
                existingTour.coverImage(),
                calculatedRoute.route(),
                existingTour.computedAttributes(),
                existingTour.createdAt(),
                updatedAt,
                nextVersion(existingTour.version())
        );
    }

    private TourDetailDto withCoverImage(
            TourDetailDto existingTour,
            CoverImageDto coverImage,
            Instant updatedAt
    ) {
        return new TourDetailDto(
                existingTour.id(),
                existingTour.userId(),
                existingTour.name(),
                existingTour.description(),
                existingTour.startLocation(),
                existingTour.endLocation(),
                existingTour.transportType(),
                existingTour.timezoneId(),
                existingTour.plannedDistanceM(),
                existingTour.estimatedDurationS(),
                coverImage,
                existingTour.route(),
                existingTour.computedAttributes(),
                existingTour.createdAt(),
                updatedAt,
                nextVersion(existingTour.version())
        );
    }

    private Optional<TourDetailDto> replaceComputedAttributes(
            Long tourId,
            List<TourLogDto> logs,
            Instant updatedAt,
            Long version
    ) {
        TourDetailDto existingTour = toursById.get(tourId);
        if (existingTour == null) {
            return Optional.empty();
        }

        TourDetailDto updatedTour = withComputedAttributes(
                existingTour,
                tourAttributeCalculator.calculate(logs),
                updatedAt,
                version
        );
        toursById.put(tourId, updatedTour);
        LOGGER.debug("Refreshed computed attributes for intermediate tour tourId={} logCount={}", tourId, logs.size());
        return Optional.of(updatedTour);
    }

    private TourDetailDto withComputedAttributes(
            TourDetailDto existingTour,
            ComputedTourAttributesDto computedAttributes,
            Instant updatedAt,
            Long version
    ) {
        return new TourDetailDto(
                existingTour.id(),
                existingTour.userId(),
                existingTour.name(),
                existingTour.description(),
                existingTour.startLocation(),
                existingTour.endLocation(),
                existingTour.transportType(),
                existingTour.timezoneId(),
                existingTour.plannedDistanceM(),
                existingTour.estimatedDurationS(),
                existingTour.coverImage(),
                existingTour.route(),
                computedAttributes,
                existingTour.createdAt(),
                updatedAt,
                version
        );
    }

    private void deletePreviousCoverImage(Long tourId, CoverImageDto previousCoverImage) {
        try {
            deleteStoredCoverImage(previousCoverImage);
        } catch (FileStorageException exception) {
            LOGGER.warn("Could not delete previous cover image for tour {}", tourId, exception);
        }
    }

    private void deleteStoredCoverImage(CoverImageDto coverImage) {
        if (coverImage != null) {
            coverImageStorageService.delete(coverImage.path());
        }
    }

    private Long nextVersion(Long version) {
        return version == null ? 1L : version + 1;
    }

    private ComputedTourAttributesDto defaultComputedAttributes() {
        return tourAttributeCalculator.calculate(List.of());
    }

    private static CoordinateDto coordinate(String latitude, String longitude) {
        return new CoordinateDto(new BigDecimal(latitude), new BigDecimal(longitude));
    }

    private static BigDecimal meters(String meters) {
        return new BigDecimal(meters);
    }

}
