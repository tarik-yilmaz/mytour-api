package org.fhtw.mytourapi.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record StoredCoverImage(
        Resource resource,
        String originalFilename,
        MediaType contentType,
        long sizeBytes
) {
}
