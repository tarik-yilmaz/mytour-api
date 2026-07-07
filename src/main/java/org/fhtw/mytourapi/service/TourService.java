package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class TourService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TourService.class);

    private final RouteCalculationService routeCalculationService;
    private final CoverImageStorageService coverImageStorageService;
    private final TourAttributeCalculator tourAttributeCalculator;
    private final TourSearchIndex tourSearchIndex;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final TourPersistenceMapper persistenceMapper;
    private final CurrentUserService currentUserService;

    public TourService(
            RouteCalculationService routeCalculationService,
            CoverImageStorageService coverImageStorageService,
            TourAttributeCalculator tourAttributeCalculator,
            TourSearchIndex tourSearchIndex,
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
    }

    @Transactional(readOnly = true)
    public TourSearchResponse searchTours(
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
                .filter((tour) -> tourMatches(tour, query, ratingMin))
                .map(persistenceMapper::toSummary)
                .sorted(Comparator.comparing(TourSummaryDto::id))
                .toList();

        LOGGER.debug(
                "Searched tours resultCount={} hasQuery={} transportType={} popularity={} childFriendliness={} ratingMin={}",
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

        Optional<Long> userId = currentUserIdIfPresent();
        if (userId.isEmpty()) {
            return List.of();
        }

        return tourRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId.get()).stream()
                .filter((tour) -> tourMatches(tour, trimmedQuery, null))
                .map(persistenceMapper::toDetail)
                .sorted(Comparator.comparing(TourDetailDto::name, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .map(this::toSuggestion)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TourDetailDto> getTour(Long tourId) {
        return currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId))
                .map(persistenceMapper::toDetail);
    }

    @Transactional(readOnly = true)
    public List<TourDetailDto> listToursForExport() {
        return currentUserIdIfPresent()
                .map((userId) -> tourRepository.findAllByUser_IdOrderByUpdatedAtDesc(userId).stream()
                .sorted(Comparator.comparing(TourEntity::getId))
                .map(persistenceMapper::toDetail)
                .toList())
                .orElseGet(List::of);
    }

    @Transactional
    public TourDetailDto createTour(CreateTourRequest request) {
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
                "Created tour tourId={} transportType={} routeSource={}",
                savedTour.getId(),
                savedTour.getTransportType(),
                savedTour.getRoute().getRouteSource()
        );
        return persistenceMapper.toDetail(savedTour);
    }

    @Transactional
    public TourDetailDto importTour(ImportedTourDto importedTour) {
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
                "Imported tour tourId={} routeSource={}",
                savedTour.getId(),
                savedTour.getRoute() == null ? null : savedTour.getRoute().getRouteSource()
        );
        return persistenceMapper.toDetail(savedTour);
    }

    @Transactional
    public Optional<TourDetailDto> updateTour(Long tourId, UpdateTourRequest request) {
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
                    LOGGER.info("Updated tour tourId={} version={}", tourId, savedTour.getVersion());
                    return persistenceMapper.toDetail(savedTour);
                });
    }

    @Transactional
    public boolean deleteTour(Long tourId) {
        Optional<TourEntity> tour = currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId));
        if (tour.isEmpty()) {
            return false;
        }

        deleteStoredCoverImage(persistenceMapper.toCoverImage(tour.get()));
        tourSearchIndex.removeTour(tourId);
        tourRepository.delete(tour.get());
        tourRepository.flush();
        LOGGER.info("Deleted tour tourId={}", tourId);
        return true;
    }

    @Transactional
    public Optional<TourRouteDto> refreshRoute(Long tourId) {
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
                            "Refreshed tour route tourId={} routeSource={}",
                            tourId,
                            savedTour.getRoute().getRouteSource()
                    );
                    return Optional.of(persistenceMapper.toRoute(savedTour.getRoute()));
                });
    }

    @Transactional
    public Optional<CoverImageDto> uploadCoverImage(Long tourId, MultipartFile file) {
        return currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId))
                .map((tour) -> {
                    CoverImageDto previousCoverImage = persistenceMapper.toCoverImage(tour);
                    CoverImageDto storedCoverImage = coverImageStorageService.store(file);
                    persistenceMapper.applyCoverImage(tour, storedCoverImage);
                    tourRepository.saveAndFlush(tour);
                    deletePreviousCoverImage(tourId, previousCoverImage);
                    LOGGER.info(
                            "Updated tour cover image tourId={} contentType={} sizeBytes={}",
                            tourId,
                            storedCoverImage.contentType(),
                            storedCoverImage.sizeBytes()
                    );
                    return storedCoverImage;
                });
    }

    @Transactional(readOnly = true)
    public Optional<StoredCoverImage> getCoverImage(Long tourId) {
        return currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId))
                .flatMap((tour) -> coverImageStorageService.load(persistenceMapper.toCoverImage(tour)));
    }

    @Transactional
    public boolean deleteCoverImage(Long tourId) {
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
        LOGGER.info("Deleted tour cover image tourId={}", tourId);
        return true;
    }

    @Transactional
    public Optional<TourDetailDto> initializeComputedAttributes(Long tourId, List<TourLogDto> logs) {
        return replacePersistedComputedAttributes(tourId, logs);
    }

    @Transactional
    public Optional<TourDetailDto> refreshComputedAttributes(Long tourId, List<TourLogDto> logs) {
        return replacePersistedComputedAttributes(tourId, logs);
    }

    Optional<Long> currentUserIdIfPresent() {
        return currentUserService.currentUserIdIfAuthenticated();
    }

    Long createOrGetCurrentUserId() {
        return currentUserService.currentUserId();
    }

    private boolean tourMatches(TourEntity tour, String query, Short ratingMin) {
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

    private Optional<TourDetailDto> replacePersistedComputedAttributes(Long tourId, List<TourLogDto> logs) {
        return currentUserIdIfPresent()
                .flatMap((userId) -> tourRepository.findByIdAndUser_Id(tourId, userId))
                .map((tour) -> {
                    ComputedTourAttributesDto computedAttributes = tourAttributeCalculator.calculate(logs);
                    persistenceMapper.applyComputedAttributes(tour, computedAttributes);
                    TourEntity savedTour = tourRepository.saveAndFlush(tour);
                    LOGGER.debug(
                            "Refreshed computed attributes for tour tourId={} logCount={}",
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

    private ComputedTourAttributesDto defaultComputedAttributes() {
        return tourAttributeCalculator.calculate(List.of());
    }
}
