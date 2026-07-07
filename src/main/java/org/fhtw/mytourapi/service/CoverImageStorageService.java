package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.config.ImageStorageProperties;
import org.fhtw.mytourapi.dto.CoverImageDto;
import org.fhtw.mytourapi.exception.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CoverImageStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoverImageStorageService.class);
    private static final String COVER_DIRECTORY = "covers";
    private static final int MAX_STORED_FILENAME_LENGTH = 140;

    private final ImageStorageProperties properties;

    public CoverImageStorageService(ImageStorageProperties properties) {
        this.properties = properties;
    }

    public CoverImageDto store(MultipartFile file) {
        validateFile(file);

        Path rootDirectory = rootDirectory();
        Path coverDirectory = resolveInsideRoot(rootDirectory, COVER_DIRECTORY);
        String originalFilename = safeOriginalFilename(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + "-" + originalFilename;
        Path target = resolveInsideRoot(coverDirectory, storedFilename);

        try {
            Files.createDirectories(coverDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not store cover image file target={}", target, exception);
            throw FileStorageException.internal("Could not store cover image file.", exception);
        }

        String storedPath = rootDirectory.relativize(target).toString().replace('\\', '/');
        CoverImageDto coverImage = new CoverImageDto(
                storedPath,
                originalFilename,
                normalizedContentType(file.getContentType()),
                file.getSize()
        );
        LOGGER.info(
                "Stored cover image file contentType={} sizeBytes={}",
                coverImage.contentType(),
                coverImage.sizeBytes()
        );
        return coverImage;
    }

    public Optional<StoredCoverImage> load(CoverImageDto coverImage) {
        if (coverImage == null || coverImage.path() == null || coverImage.path().isBlank()) {
            return Optional.empty();
        }

        Path rootDirectory = rootDirectory();
        Path target = resolveStoredPath(rootDirectory, coverImage.path());
        if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
            LOGGER.warn("Cover image file not found or not readable path={}", coverImage.path());
            return Optional.empty();
        }

        try {
            Resource resource = new FileSystemResource(target);
            return Optional.of(new StoredCoverImage(
                    resource,
                    safeOriginalFilename(coverImage.originalFilename()),
                    mediaType(coverImage.contentType()),
                    Files.size(target)
            ));
        } catch (IOException exception) {
            LOGGER.error("Could not read cover image file target={}", target, exception);
            throw FileStorageException.internal("Could not read cover image file.", exception);
        }
    }

    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }

        Path rootDirectory = rootDirectory();
        Path target = resolveStoredPath(rootDirectory, storedPath);
        try {
            boolean deleted = Files.deleteIfExists(target);
            LOGGER.info("Deleted cover image file deleted={}", deleted);
        } catch (IOException exception) {
            LOGGER.error("Could not delete cover image file target={}", target, exception);
            throw FileStorageException.internal("Could not delete cover image file.", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw FileStorageException.badRequest("Cover image file must not be empty.");
        }

        long maxSizeBytes = properties.getMaxSizeBytes();
        if (maxSizeBytes <= 0) {
            throw FileStorageException.badRequest("Cover image storage max size is not configured.");
        }

        if (file.getSize() > maxSizeBytes) {
            throw FileStorageException.badRequest("Cover image file exceeds the configured maximum size.");
        }

        String contentType = normalizedContentType(file.getContentType());
        if (!isAllowedContentType(contentType)) {
            throw FileStorageException.badRequest("Unsupported cover image content type.");
        }
    }

    private boolean isAllowedContentType(String contentType) {
        List<String> allowedContentTypes = properties.getAllowedContentTypes();
        return contentType != null
                && allowedContentTypes != null
                && allowedContentTypes.stream().anyMatch((allowed) -> contentType.equalsIgnoreCase(allowed));
    }

    private Path rootDirectory() {
        Path baseDirectory = properties.getBaseDirectory();
        if (baseDirectory == null) {
            throw FileStorageException.badRequest("Image storage base directory is not configured.");
        }

        return baseDirectory.toAbsolutePath().normalize();
    }

    private Path resolveStoredPath(Path rootDirectory, String storedPath) {
        String cleanedPath = StringUtils.cleanPath(storedPath);
        Path relativePath = Path.of(cleanedPath);
        if (relativePath.isAbsolute()) {
            throw FileStorageException.badRequest("Stored cover image path must be relative.");
        }

        return resolveInsideRoot(rootDirectory, cleanedPath);
    }

    private Path resolveInsideRoot(Path rootDirectory, String childPath) {
        Path resolvedPath = rootDirectory.resolve(childPath).normalize();
        if (!resolvedPath.startsWith(rootDirectory)) {
            throw FileStorageException.badRequest("Stored cover image path escapes the configured image directory.");
        }

        return resolvedPath;
    }

    private static String normalizedContentType(String contentType) {
        return contentType == null ? null : contentType.trim().toLowerCase();
    }

    private static MediaType mediaType(String contentType) {
        String normalizedContentType = normalizedContentType(contentType);
        if (normalizedContentType == null || normalizedContentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(normalizedContentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String safeOriginalFilename(String originalFilename) {
        String candidate = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int lastSeparator = Math.max(candidate.lastIndexOf('/'), candidate.lastIndexOf('\\'));
        String filename = lastSeparator >= 0 ? candidate.substring(lastSeparator + 1) : candidate;
        String safeFilename = filename.replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^\\.+", "");

        if (safeFilename.isBlank()) {
            safeFilename = "cover-image";
        }

        if (safeFilename.length() > MAX_STORED_FILENAME_LENGTH) {
            return safeFilename.substring(0, MAX_STORED_FILENAME_LENGTH);
        }

        return safeFilename;
    }
}
