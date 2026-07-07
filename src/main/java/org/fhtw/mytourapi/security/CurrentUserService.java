package org.fhtw.mytourapi.security;

import org.fhtw.mytourapi.domain.UserEntity;
import org.fhtw.mytourapi.dto.UserDto;
import org.fhtw.mytourapi.exception.UnauthorizedException;
import org.fhtw.mytourapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentUserService.class);

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<Long> currentUserIdIfAuthenticated() {
        return currentPrincipal().map(AuthenticatedUserPrincipal::userId);
    }

    public Long currentUserId() {
        return currentUserIdIfAuthenticated()
                .orElseThrow(() -> {
                    LOGGER.warn("Authentication required but no authenticated user found");
                    return new UnauthorizedException("Authentication required");
                });
    }

    public UserEntity currentUser() {
        Long userId = currentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    LOGGER.warn("Authenticated user not found in database userId={}", userId);
                    return new UnauthorizedException("Authentication required");
                });
    }

    public UserDto currentUserDto() {
        UserEntity user = currentUser();
        return new UserDto(user.getId(), user.getUsername(), user.getCreatedAt());
    }

    private Optional<AuthenticatedUserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUserPrincipal authenticatedUser) {
            return Optional.of(authenticatedUser);
        }

        return Optional.empty();
    }
}
