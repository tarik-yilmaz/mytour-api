package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.CoordinateDto;
import org.fhtw.mytourapi.dto.CreateTourLogRequest;
import org.fhtw.mytourapi.dto.CreateTourRequest;
import org.fhtw.mytourapi.dto.DemoDataResultDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.fhtw.mytourapi.dto.TransportType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class DemoDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataService.class);

    private final TourService tourService;
    private final TourLogService tourLogService;

    public DemoDataService(TourService tourService, TourLogService tourLogService) {
        this.tourService = tourService;
        this.tourLogService = tourLogService;
    }

    public DemoDataResultDto seedDemoData() {
        deleteAllToursForCurrentUser();

        int tourCount = 0;
        int logCount = 0;

        TourDetailDto bikeTour = tourService.createTour(bikeTourRequest());
        tourCount++;
        logCount += createLogs(bikeTour.id(), bikeLogs());

        TourDetailDto hikeTour = tourService.createTour(hikeTourRequest());
        tourCount++;
        logCount += createLogs(hikeTour.id(), hikeLogs());

        TourDetailDto runningTour = tourService.createTour(runningTourRequest());
        tourCount++;
        logCount += createLogs(runningTour.id(), runningLogs());

        LOGGER.info("Seeded demo data tourCount={} logCount={}", tourCount, logCount);
        return new DemoDataResultDto(tourCount, logCount);
    }

    private void deleteAllToursForCurrentUser() {
        List<Long> tourIds = tourService.listToursForExport().stream()
                .map(TourDetailDto::id)
                .toList();

        for (Long tourId : tourIds) {
            tourService.deleteTour(tourId);
        }

        if (!tourIds.isEmpty()) {
            LOGGER.info("Deleted existing tours before seeding demo data count={}", tourIds.size());
        }
    }

    private int createLogs(Long tourId, List<CreateTourLogRequest> logs) {
        int created = 0;
        for (CreateTourLogRequest log : logs) {
            Optional<TourLogDto> result = tourLogService.createLog(tourId, log);
            if (result.isPresent()) {
                created++;
            }
        }
        return created;
    }

    private CreateTourRequest bikeTourRequest() {
        return new CreateTourRequest(
                "Donauinsel Radtour",
                "A scenic bike ride along the Danube from Prater to Donauinsel.",
                "Wien Praterstern",
                "Donauinsel Nord",
                TransportType.BIKE,
                "Europe/Vienna",
                new CoordinateDto(new BigDecimal("48.2092"), new BigDecimal("16.4044")),
                new CoordinateDto(new BigDecimal("48.2500"), new BigDecimal("16.4000"))
        );
    }

    private List<CreateTourLogRequest> bikeLogs() {
        return List.of(
                new CreateTourLogRequest(
                        daysAgoAtHour(7, 8),
                        "Beautiful morning ride along the Danube. Great weather!",
                        (short) 2,
                        new BigDecimal("9800"),
                        2100,
                        (short) 5
                ),
                new CreateTourLogRequest(
                        daysAgoAtHour(14, 9),
                        "A bit windy but still enjoyable. The island path is flat and fast.",
                        (short) 2,
                        new BigDecimal("10200"),
                        2280,
                        (short) 4
                ),
                new CreateTourLogRequest(
                        daysAgoAtHour(21, 10),
                        "Weekend ride with friends. Perfect conditions for cycling.",
                        (short) 1,
                        new BigDecimal("9500"),
                        1950,
                        (short) 5
                )
        );
    }

    private CreateTourRequest hikeTourRequest() {
        return new CreateTourRequest(
                "Kahlenberg Wanderung",
                "Classic Vienna hike with panoramic views over the city and Danube valley.",
                "Kahlenberg",
                "Leopoldsberg",
                TransportType.HIKE,
                "Europe/Vienna",
                new CoordinateDto(new BigDecimal("48.2650"), new BigDecimal("16.3540")),
                new CoordinateDto(new BigDecimal("48.2760"), new BigDecimal("16.3430"))
        );
    }

    private List<CreateTourLogRequest> hikeLogs() {
        return List.of(
                new CreateTourLogRequest(
                        daysAgoAtHour(7, 14),
                        "Stunning views from the lookout platform. Worth the climb.",
                        (short) 3,
                        new BigDecimal("4200"),
                        5400,
                        (short) 5
                ),
                new CreateTourLogRequest(
                        daysAgoAtHour(14, 15),
                        "Started late afternoon, caught the sunset from Leopoldsberg.",
                        (short) 3,
                        new BigDecimal("4500"),
                        5700,
                        (short) 4
                ),
                new CreateTourLogRequest(
                        daysAgoAtHour(21, 13),
                        "Hiked with the family. Kids managed the trail well.",
                        (short) 2,
                        new BigDecimal("4000"),
                        6600,
                        (short) 4
                ),
                new CreateTourLogRequest(
                        daysAgoAtHour(28, 16),
                        "Cloudy day but still beautiful. The vineyards along the way are lovely.",
                        (short) 4,
                        new BigDecimal("4600"),
                        6000,
                        (short) 3
                )
        );
    }

    private CreateTourRequest runningTourRequest() {
        return new CreateTourRequest(
                "Stadtpark Morning Run",
                "Quick morning run through Vienna's historic city center and Stadtpark.",
                "Wien Stadtpark",
                "Schwarzenbergplatz",
                TransportType.RUNNING,
                "Europe/Vienna",
                new CoordinateDto(new BigDecimal("48.2040"), new BigDecimal("16.3770")),
                new CoordinateDto(new BigDecimal("48.1990"), new BigDecimal("16.3700"))
        );
    }

    private List<CreateTourLogRequest> runningLogs() {
        return List.of(
                new CreateTourLogRequest(
                        daysAgoAtHour(7, 6),
                        "Early morning run, empty streets and crisp air.",
                        (short) 2,
                        new BigDecimal("3200"),
                        1020,
                        (short) 4
                ),
                new CreateTourLogRequest(
                        daysAgoAtHour(14, 7),
                        "Felt energetic today, pushed the pace a bit.",
                        (short) 2,
                        new BigDecimal("3100"),
                        960,
                        (short) 5
                ),
                new CreateTourLogRequest(
                        daysAgoAtHour(21, 6),
                        "Relaxed recovery run through the park. Nice and easy.",
                        (short) 1,
                        new BigDecimal("3400"),
                        1200,
                        (short) 3
                )
        );
    }

    private Instant daysAgoAtHour(int daysAgo, int hourUtc) {
        return Instant.now()
                .truncatedTo(ChronoUnit.DAYS)
                .minus(daysAgo, ChronoUnit.DAYS)
                .plus(hourUtc, ChronoUnit.HOURS);
    }
}
