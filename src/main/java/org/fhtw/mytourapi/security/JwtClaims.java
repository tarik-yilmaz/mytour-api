package org.fhtw.mytourapi.security;

import java.time.Instant;

public record JwtClaims(
        Long userId,
        String username,
        Instant expiresAt
) {
}
