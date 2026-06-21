package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.config.OpenRouteServiceProperties;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.dto.TourDetailDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class IntermediateTourServiceCoverImageTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void uploadCoverImageStoresMetadataAndDeleteClearsIt() {
        IntermediateTourService service = tourService();
        MockMultipartFile file = imageFile("cover.png", "image/png", "first-image");

        Optional<CoverImageDto> uploaded = service.uploadCoverImage(2L, file);

        assertThat(uploaded).isPresent();
        TourDetailDto tourAfterUpload = service.getTour(2L).orElseThrow();
        assertThat(tourAfterUpload.coverImage()).isEqualTo(uploaded.get());
        assertThat(tourAfterUpload.version()).isEqualTo(2L);
        assertThat(Files.exists(tempDirectory.resolve(uploaded.get().path()))).isTrue();

        assertThat(service.deleteCoverImage(2L)).isTrue();

        TourDetailDto tourAfterDelete = service.getTour(2L).orElseThrow();
        assertThat(tourAfterDelete.coverImage()).isNull();
        assertThat(tourAfterDelete.version()).isEqualTo(3L);
        assertThat(Files.exists(tempDirectory.resolve(uploaded.get().path()))).isFalse();
    }

    @Test
    void replacingCoverImageRemovesPreviousFile() {
        IntermediateTourService service = tourService();
        CoverImageDto firstImage = service.uploadCoverImage(2L, imageFile("first.png", "image/png", "first"))
                .orElseThrow();
        CoverImageDto secondImage = service.uploadCoverImage(2L, imageFile("second.webp", "image/webp", "second"))
                .orElseThrow();

        assertThat(Files.exists(tempDirectory.resolve(firstImage.path()))).isFalse();
        assertThat(Files.exists(tempDirectory.resolve(secondImage.path()))).isTrue();
        assertThat(service.getTour(2L).orElseThrow().coverImage()).isEqualTo(secondImage);
    }

    @Test
    void uploadCoverImageReturnsEmptyForMissingTour() {
        IntermediateTourService service = tourService();

        Optional<CoverImageDto> uploaded = service.uploadCoverImage(
                999L,
                imageFile("missing.png", "image/png", "missing")
        );

        assertThat(uploaded).isEmpty();
        assertThat(tempDirectory).isEmptyDirectory();
    }

    private IntermediateTourService tourService() {
        return new IntermediateTourService(
                routeCalculationService(),
                coverImageStorageService(),
                new TourAttributeCalculator(),
                new IntermediateTourSearchIndex()
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

    private static MockMultipartFile imageFile(String filename, String contentType, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                contentType,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
