package org.fhtw.mytourapi.service;

import org.fhtw.mytourapi.domain.UserEntity;
import org.fhtw.mytourapi.dto.AuthResponse;
import org.fhtw.mytourapi.dto.LoginRequest;
import org.fhtw.mytourapi.dto.RegisterRequest;
import org.fhtw.mytourapi.dto.UserDto;
import org.fhtw.mytourapi.exception.ConflictException;
import org.fhtw.mytourapi.exception.UnauthorizedException;
import org.fhtw.mytourapi.repository.UserRepository;
import org.fhtw.mytourapi.security.CurrentUserService;
import org.fhtw.mytourapi.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CurrentUserService currentUserService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String normalizedUsername = normalizeUsername(username);
        if (userRepository.existsByUsernameNormalized(normalizedUsername)) {
            throw new ConflictException("Username is already taken");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setUsernameNormalized(normalizedUsername);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        try {
            UserEntity savedUser = userRepository.saveAndFlush(user);
            LOGGER.info("Registered user userId={} usernameNormalized={}", savedUser.getId(), savedUser.getUsernameNormalized());
            return authResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Username is already taken", exception);
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedUsername = normalizeUsername(request.username());
        UserEntity user = userRepository.findByUsernameNormalized(normalizedUsername)
                .orElseThrow(AuthService::invalidCredentials);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }

        LOGGER.info("User login succeeded userId={} usernameNormalized={}", user.getId(), user.getUsernameNormalized());
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto currentUser() {
        return currentUserService.currentUserDto();
    }

    private AuthResponse authResponse(UserEntity user) {
        JwtService.IssuedJwt jwt = jwtService.issueToken(user);
        return new AuthResponse(
                jwt.token(),
                TOKEN_TYPE,
                jwt.expiresAt(),
                new UserDto(user.getId(), user.getUsername(), user.getCreatedAt())
        );
    }

    private static UnauthorizedException invalidCredentials() {
        return new UnauthorizedException("Invalid username or password");
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
