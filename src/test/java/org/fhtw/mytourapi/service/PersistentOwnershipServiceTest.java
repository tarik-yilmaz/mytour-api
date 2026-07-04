package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.domain.ChildFriendlinessCategory;
import org.fhtw.mytourapi.domain.PopularityCategory;
import org.fhtw.mytourapi.domain.TourEntity;
import org.fhtw.mytourapi.domain.TourLogEntity;
import org.fhtw.mytourapi.domain.TransportType;
import org.fhtw.mytourapi.domain.UserEntity;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.mapper.TourPersistenceMapper;
import org.fhtw.mytourapi.repository.TourLogRepository;
import org.fhtw.mytourapi.repository.TourRepository;
import org.fhtw.mytourapi.repository.UserRepository;
import org.fhtw.mytourapi.security.AuthenticatedUserPrincipal;
import org.fhtw.mytourapi.security.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentOwnershipServiceTest {

    @TempDir
    private Path tempDirectory;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTourUsesCurrentUserOwnershipScope() {
        UserEntity user = user(42L);
        TourEntity tour = tour(7L, user);
        RepositoryStub<TourRepository> tourRepository = RepositoryStub.create(TourRepository.class);
        RepositoryStub<UserRepository> userRepository = RepositoryStub.create(UserRepository.class);
        IntermediateTourService tourService = persistentTourService(tourRepository.proxy(), userRepository.proxy());

        authenticate(user);
        tourRepository.respond("findByIdAndUser_Id", Optional.of(tour), 7L, 42L);

        Optional<TourDetailDto> result = tourService.getTour(7L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().id()).isEqualTo(7L);
        assertThat(tourRepository.called("findByIdAndUser_Id", 7L, 42L)).isTrue();
    }

    @Test
    void listLogsUsesTourAndCurrentUserOwnershipScope() {
        UserEntity user = user(42L);
        TourEntity tour = tour(7L, user);
        TourLogEntity log = log(11L, tour);
        RepositoryStub<TourRepository> tourRepository = RepositoryStub.create(TourRepository.class);
        RepositoryStub<TourLogRepository> tourLogRepository = RepositoryStub.create(TourLogRepository.class);
        RepositoryStub<UserRepository> userRepository = RepositoryStub.create(UserRepository.class);
        IntermediateTourService tourService = persistentTourService(tourRepository.proxy(), userRepository.proxy());
        IntermediateTourLogService logService = persistentLogService(
                tourService,
                tourRepository.proxy(),
                tourLogRepository.proxy()
        );

        authenticate(user);
        tourRepository.respond("existsByIdAndUser_Id", true, 7L, 42L);
        tourLogRepository.respond(
                "findAllByTour_IdAndTour_User_IdOrderByPerformedAtDesc",
                List.of(log),
                7L,
                42L
        );

        Optional<List<TourLogDto>> result = logService.listLogs(7L);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).extracting(TourLogDto::id).containsExactly(11L);
        assertThat(tourRepository.called("existsByIdAndUser_Id", 7L, 42L)).isTrue();
        assertThat(tourLogRepository.called(
                "findAllByTour_IdAndTour_User_IdOrderByPerformedAtDesc",
                7L,
                42L
        )).isTrue();
    }

    @Test
    void deleteLogDoesNotDeleteWhenOwnershipScopedLookupMisses() {
        UserEntity user = user(42L);
        RepositoryStub<TourRepository> tourRepository = RepositoryStub.create(TourRepository.class);
        RepositoryStub<TourLogRepository> tourLogRepository = RepositoryStub.create(TourLogRepository.class);
        RepositoryStub<UserRepository> userRepository = RepositoryStub.create(UserRepository.class);
        IntermediateTourService tourService = persistentTourService(tourRepository.proxy(), userRepository.proxy());
        IntermediateTourLogService logService = persistentLogService(
                tourService,
                tourRepository.proxy(),
                tourLogRepository.proxy()
        );

        authenticate(user);
        tourLogRepository.respond("findByIdAndTour_IdAndTour_User_Id", Optional.empty(), 11L, 7L, 42L);

        assertThat(logService.deleteLog(7L, 11L)).isFalse();

        assertThat(tourLogRepository.called("findByIdAndTour_IdAndTour_User_Id", 11L, 7L, 42L)).isTrue();
        assertThat(tourLogRepository.called("delete")).isFalse();
    }

    private IntermediateTourService persistentTourService(
            TourRepository tourRepository,
            UserRepository userRepository
    ) {
        return new IntermediateTourService(
                routeCalculationService(),
                coverImageStorageService(),
                new TourAttributeCalculator(),
                new IntermediateTourSearchIndex(),
                tourRepository,
                userRepository,
                new TourPersistenceMapper(),
                new CurrentUserService(userRepository)
        );
    }

    private IntermediateTourLogService persistentLogService(
            IntermediateTourService tourService,
            TourRepository tourRepository,
            TourLogRepository tourLogRepository
    ) {
        return new IntermediateTourLogService(
                tourService,
                new IntermediateTourSearchIndex(),
                WeatherSnapshotService.localFallback(),
                tourRepository,
                tourLogRepository,
                new TourPersistenceMapper()
        );
    }

    private RouteCalculationService routeCalculationService() {
        return new RouteCalculationService(
                new OpenRouteServiceProperties(),
                (profile, startCoordinate, endCoordinate, fetchedAt) -> {
                    throw new AssertionError("OpenRouteService client must not be used without an API key.");
                }
        );
    }

    private CoverImageStorageService coverImageStorageService() {
        ImageStorageProperties properties = new ImageStorageProperties();
        properties.setBaseDirectory(tempDirectory);
        return new CoverImageStorageService(properties);
    }

    private static void authenticate(UserEntity user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(user.getId(), user.getUsername()),
                null,
                List.of()
        ));
    }

    private static UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("Alice");
        user.setUsernameNormalized("alice");
        user.setPasswordHash("{noop}password");
        return user;
    }

    private static TourEntity tour(Long id, UserEntity user) {
        TourEntity tour = new TourEntity();
        tour.setId(id);
        tour.setUser(user);
        tour.setName("Ownership Tour");
        tour.setDescription("Ownership checked");
        tour.setStartLocation("Start");
        tour.setEndLocation("End");
        tour.setTransportType(TransportType.BIKE);
        tour.setTimezoneId("Europe/Vienna");
        tour.setPlannedDistanceM(new BigDecimal("1200"));
        tour.setEstimatedDurationS(900);
        tour.setLogCount(0);
        tour.setPopularityScore(0);
        tour.setPopularityCategory(PopularityCategory.NEW);
        tour.setPopularityLabel("new");
        tour.setChildFriendlinessScore(0);
        tour.setChildFriendlinessCategory(ChildFriendlinessCategory.UNKNOWN);
        tour.setChildFriendlinessLabel("unknown");
        tour.setCreatedAt(Instant.parse("2026-06-21T10:00:00Z"));
        tour.setUpdatedAt(Instant.parse("2026-06-21T10:00:00Z"));
        tour.setVersion(1L);
        return tour;
    }

    private static TourLogEntity log(Long id, TourEntity tour) {
        TourLogEntity log = new TourLogEntity();
        log.setId(id);
        log.setTour(tour);
        log.setPerformedAt(Instant.parse("2026-06-21T11:00:00Z"));
        log.setComment("Scoped log");
        log.setDifficulty((short) 2);
        log.setTotalDistanceM(new BigDecimal("1200"));
        log.setTotalTimeS(900);
        log.setRating((short) 5);
        log.setCreatedAt(Instant.parse("2026-06-21T11:05:00Z"));
        log.setUpdatedAt(Instant.parse("2026-06-21T11:05:00Z"));
        log.setVersion(1L);
        return log;
    }

    private record Invocation(String methodName, List<Object> arguments) {
    }

    private static final class RepositoryStub<T> implements InvocationHandler {

        private final T proxy;
        private final List<Invocation> invocations = new ArrayList<>();
        private final Map<Invocation, Object> responses = new HashMap<>();

        private RepositoryStub(Class<T> repositoryType) {
            Object createdProxy = Proxy.newProxyInstance(
                    repositoryType.getClassLoader(),
                    new Class<?>[]{repositoryType},
                    this
            );
            this.proxy = repositoryType.cast(createdProxy);
        }

        static <T> RepositoryStub<T> create(Class<T> repositoryType) {
            return new RepositoryStub<>(repositoryType);
        }

        T proxy() {
            return proxy;
        }

        void respond(String methodName, Object response, Object... arguments) {
            responses.put(invocation(methodName, arguments), response);
        }

        boolean called(String methodName, Object... arguments) {
            if (arguments.length == 0) {
                return invocations.stream().anyMatch((invocation) -> invocation.methodName().equals(methodName));
            }

            return invocations.contains(invocation(methodName, arguments));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(method, arguments);
            }

            Invocation invocation = invocation(method.getName(), arguments == null ? new Object[0] : arguments);
            invocations.add(invocation);

            if (responses.containsKey(invocation)) {
                return responses.get(invocation);
            }

            return defaultValue(method.getReturnType());
        }

        private Object objectMethod(Method method, Object[] arguments) {
            return switch (method.getName()) {
                case "toString" -> "RepositoryStub[" + proxy.getClass().getInterfaces()[0].getSimpleName() + "]";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == Void.TYPE) {
                return null;
            }
            if (returnType == Boolean.TYPE) {
                return false;
            }
            if (returnType == Long.TYPE) {
                return 0L;
            }
            if (returnType == Integer.TYPE) {
                return 0;
            }
            if (returnType == Optional.class) {
                return Optional.empty();
            }
            if (returnType == List.class) {
                return List.of();
            }
            return null;
        }

        private Invocation invocation(String methodName, Object... arguments) {
            return new Invocation(methodName, Arrays.stream(arguments)
                    .map((argument) -> argument == null ? NullArgument.INSTANCE : argument)
                    .toList());
        }
    }

    private enum NullArgument {
        INSTANCE
    }
}
