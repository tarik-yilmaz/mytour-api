package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.exception.FileStorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoverImageStorageServiceTest {

    @TempDir
    private Path tempDirectory;

    @Test
    void storeWritesCoverImageBelowConfiguredDirectoryAndReturnsMetadata() throws Exception {
        CoverImageStorageService service = storageService();
        byte[] content = "image-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../City Trail.JPG",
                "image/jpeg",
                content
        );

        CoverImageDto result = service.store(file);

        assertThat(result.path())
                .startsWith("covers/")
                .endsWith("-City_Trail.JPG")
                .doesNotContain("..");
        assertThat(result.originalFilename()).isEqualTo("City_Trail.JPG");
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.sizeBytes()).isEqualTo((long) content.length);

        Path storedFile = tempDirectory.resolve(result.path()).normalize();
        assertThat(storedFile).startsWith(tempDirectory);
        assertThat(Files.readAllBytes(storedFile)).isEqualTo(content);
    }

    @Test
    void loadReturnsStoredCoverImageResource() throws Exception {
        CoverImageStorageService service = storageService();
        byte[] content = "image-bytes".getBytes(StandardCharsets.UTF_8);
        CoverImageDto storedImage = service.store(new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                content
        ));

        StoredCoverImage result = service.load(storedImage).orElseThrow();

        assertThat(result.originalFilename()).isEqualTo("cover.png");
        assertThat(result.contentType().toString()).isEqualTo("image/png");
        assertThat(result.sizeBytes()).isEqualTo((long) content.length);
        assertThat(result.resource().getInputStream().readAllBytes()).isEqualTo(content);
    }

    @Test
    void storeRejectsUnsupportedContentType() {
        CoverImageStorageService service = storageService();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "not-an-image".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("Unsupported cover image content type.");
    }

    @Test
    void deleteRejectsPathsOutsideConfiguredDirectory() {
        CoverImageStorageService service = storageService();

        assertThatThrownBy(() -> service.delete("../outside.jpg"))
                .isInstanceOf(FileStorageException.class)
                .hasMessage("Stored cover image path escapes the configured image directory.");
    }

    private CoverImageStorageService storageService() {
        ImageStorageProperties properties = new ImageStorageProperties();
        properties.setBaseDirectory(tempDirectory);
        return new CoverImageStorageService(properties);
    }
}
