package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.dto.ChildFriendlinessCategory;
import org.fhtw.mytourapi.dto.ComputedTourAttributesDto;
import org.fhtw.mytourapi.dto.PopularityCategory;
import org.fhtw.mytourapi.dto.TourLogDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class TourAttributeCalculator {

    private static final double MAX_FAMILY_DISTANCE_M = 20_000.0;
    private static final double MAX_FAMILY_DURATION_S = 10_800.0;

    public ComputedTourAttributesDto calculate(List<TourLogDto> logs) {
        List<TourLogDto> safeLogs = logs == null
                ? List.of()
                : logs.stream().filter(Objects::nonNull).toList();
        int logCount = safeLogs.size();
        int popularityScore = popularityScore(logCount);
        PopularityCategory popularityCategory = popularityCategory(logCount);

        if (logCount == 0) {
            return new ComputedTourAttributesDto(
                    0,
                    popularityScore,
                    popularityCategory,
                    popularityLabel(popularityCategory),
                    0,
                    ChildFriendlinessCategory.UNKNOWN,
                    childFriendlinessLabel(ChildFriendlinessCategory.UNKNOWN)
            );
        }

        int childFriendlinessScore = childFriendlinessScore(safeLogs);
        ChildFriendlinessCategory childFriendlinessCategory = childFriendlinessCategory(childFriendlinessScore);

        return new ComputedTourAttributesDto(
                logCount,
                popularityScore,
                popularityCategory,
                popularityLabel(popularityCategory),
                childFriendlinessScore,
                childFriendlinessCategory,
                childFriendlinessLabel(childFriendlinessCategory)
        );
    }

    private int popularityScore(int logCount) {
        return Math.min(100, logCount * 20);
    }

    private PopularityCategory popularityCategory(int logCount) {
        if (logCount == 0) {
            return PopularityCategory.NEW;
        }

        if (logCount <= 2) {
            return PopularityCategory.RARELY_USED;
        }

        if (logCount <= 4) {
            return PopularityCategory.POPULAR;
        }

        return PopularityCategory.VERY_POPULAR;
    }

    private String popularityLabel(PopularityCategory category) {
        return switch (category) {
            case NEW -> "new";
            case RARELY_USED -> "rarely used";
            case POPULAR -> "popular";
            case VERY_POPULAR -> "very popular";
        };
    }

    private int childFriendlinessScore(List<TourLogDto> logs) {
        double averageDifficulty = logs.stream()
                .map(TourLogDto::difficulty)
                .filter(Objects::nonNull)
                .mapToDouble(Short::doubleValue)
                .average()
                .orElse(5.0);
        double averageDistanceM = logs.stream()
                .map(TourLogDto::totalDistanceM)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(MAX_FAMILY_DISTANCE_M);
        double averageTimeS = logs.stream()
                .map(TourLogDto::totalTimeS)
                .filter(Objects::nonNull)
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(MAX_FAMILY_DURATION_S);

        double difficultyScore = clamp(100.0 - ((averageDifficulty - 1.0) / 4.0 * 100.0));
        double distanceScore = clamp(100.0 - (averageDistanceM / MAX_FAMILY_DISTANCE_M * 100.0));
        double timeScore = clamp(100.0 - (averageTimeS / MAX_FAMILY_DURATION_S * 100.0));

        return (int) Math.round(
                difficultyScore * 0.50
                        + distanceScore * 0.25
                        + timeScore * 0.25
        );
    }

    private ChildFriendlinessCategory childFriendlinessCategory(int score) {
        if (score >= 75) {
            return ChildFriendlinessCategory.FAMILY_FRIENDLY;
        }

        if (score >= 50) {
            return ChildFriendlinessCategory.MODERATE_FAMILY_SUITABILITY;
        }

        if (score >= 25) {
            return ChildFriendlinessCategory.CHALLENGING_ROUTE;
        }

        return ChildFriendlinessCategory.ADULT_ORIENTED;
    }

    private String childFriendlinessLabel(ChildFriendlinessCategory category) {
        return switch (category) {
            case UNKNOWN -> "unknown";
            case FAMILY_FRIENDLY -> "family friendly";
            case MODERATE_FAMILY_SUITABILITY -> "moderate family suitability";
            case CHALLENGING_ROUTE -> "challenging route";
            case ADULT_ORIENTED -> "adult oriented";
        };
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
