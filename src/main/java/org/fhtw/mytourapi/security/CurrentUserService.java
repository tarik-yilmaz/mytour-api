package org.fhtw.mytourapi.security;

import org.fhtw.mytourapi.domain.UserEntity;
import org.fhtw.mytourapi.dto.UserDto;
import org.fhtw.mytourapi.repository.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    @Autowired
    public CurrentUserService(ObjectProvider<UserRepository> userRepositoryProvider) {
        this.userRepository = userRepositoryProvider.getIfAvailable();
    }

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<Long> currentUserIdIfAuthenticated() {
        return currentPrincipal().map(AuthenticatedUserPrincipal::userId);
    }

    public Long currentUserId() {
        return currentUserIdIfAuthenticated()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }

    public UserEntity currentUser() {
        if (userRepository == null) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "User repository is not available");
        }

        Long userId = currentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
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
