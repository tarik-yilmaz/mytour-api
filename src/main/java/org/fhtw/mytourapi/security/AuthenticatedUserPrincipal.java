package org.fhtw.mytourapi.security;

public record AuthenticatedUserPrincipal(
        Long userId,
        String username
) {
}
