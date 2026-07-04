package org.fhtw.mytourapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fhtw.mytourapi.domain.UserEntity;
import org.fhtw.mytourapi.dto.AuthResponse;
import org.fhtw.mytourapi.dto.LoginRequest;
import org.fhtw.mytourapi.dto.RegisterRequest;
import org.fhtw.mytourapi.dto.UserDto;
import org.fhtw.mytourapi.repository.UserRepository;
import org.fhtw.mytourapi.security.AuthenticatedUserPrincipal;
import org.fhtw.mytourapi.security.CurrentUserService;
import org.fhtw.mytourapi.security.JwtClaims;
import org.fhtw.mytourapi.security.JwtProperties;
import org.fhtw.mytourapi.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerHashesPasswordAndReturnsBearerJwt() {
        UserRepositoryStub repository = new UserRepositoryStub();
        JwtService jwtService = jwtService();
        AuthService authService = authService(repository.proxy(), jwtService);

        AuthResponse response = authService.register(new RegisterRequest("Alice_1", "very-secret"));

        UserEntity savedUser = repository.userByNormalized("alice_1").orElseThrow();
        JwtClaims claims = jwtService.parse(response.accessToken());
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().id()).isEqualTo(savedUser.getId());
        assertThat(response.user().username()).isEqualTo("Alice_1");
        assertThat(claims.userId()).isEqualTo(savedUser.getId());
        assertThat(claims.username()).isEqualTo("Alice_1");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("very-secret");
        assertThat(passwordEncoder.matches("very-secret", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    void loginRejectsInvalidPassword() {
        UserRepositoryStub repository = new UserRepositoryStub();
        AuthService authService = authService(repository.proxy(), jwtService());
        authService.register(new RegisterRequest("alice", "very-secret"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ALICE", "wrong-secret")))
                .isInstanceOfSatisfying(ResponseStatusException.class, (exception) ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void currentUserReturnsAuthenticatedUserProfile() {
        UserRepositoryStub repository = new UserRepositoryStub();
        AuthService authService = authService(repository.proxy(), jwtService());
        AuthResponse registered = authService.register(new RegisterRequest("alice", "very-secret"));
        authenticate(registered.user());

        UserDto currentUser = authService.currentUser();

        assertThat(currentUser.id()).isEqualTo(registered.user().id());
        assertThat(currentUser.username()).isEqualTo("alice");
    }

    private AuthService authService(UserRepository userRepository, JwtService jwtService) {
        return new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                new CurrentUserService(userRepository)
        );
    }

    private JwtService jwtService() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-with-more-than-thirty-two-characters");
        properties.setIssuer("mytour-api-test");
        properties.setExpiration(Duration.ofHours(1));
        return new JwtService(properties, new ObjectMapper());
    }

    private static void authenticate(UserDto user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUserPrincipal(user.id(), user.username()),
                null,
                List.of()
        ));
    }

    private static final class UserRepositoryStub implements InvocationHandler {

        private final UserRepository proxy;
        private final Map<Long, UserEntity> usersById = new HashMap<>();
        private final Map<String, UserEntity> usersByNormalized = new HashMap<>();
        private long nextId = 1L;

        private UserRepositoryStub() {
            this.proxy = (UserRepository) Proxy.newProxyInstance(
                    UserRepository.class.getClassLoader(),
                    new Class<?>[]{UserRepository.class},
                    this
            );
        }

        UserRepository proxy() {
            return proxy;
        }

        Optional<UserEntity> userByNormalized(String usernameNormalized) {
            return Optional.ofNullable(usersByNormalized.get(usernameNormalized));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] arguments) {
            if (method.getDeclaringClass() == Object.class) {
                return objectMethod(method, arguments);
            }

            return switch (method.getName()) {
                case "existsByUsernameNormalized" -> usersByNormalized.containsKey(arguments[0]);
                case "findByUsernameNormalized" -> Optional.ofNullable(usersByNormalized.get(arguments[0]));
                case "findById" -> Optional.ofNullable(usersById.get(arguments[0]));
                case "saveAndFlush" -> save((UserEntity) arguments[0]);
                default -> defaultValue(method.getReturnType());
            };
        }

        private UserEntity save(UserEntity user) {
            if (user.getId() == null) {
                user.setId(nextId++);
            }
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(Instant.parse("2026-06-21T10:00:00Z"));
            }
            if (user.getUpdatedAt() == null) {
                user.setUpdatedAt(user.getCreatedAt());
            }
            if (user.getVersion() == null) {
                user.setVersion(0L);
            }

            usersById.put(user.getId(), user);
            usersByNormalized.put(user.getUsernameNormalized(), user);
            return user;
        }

        private Object objectMethod(Method method, Object[] arguments) {
            return switch (method.getName()) {
                case "toString" -> "UserRepositoryStub";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == arguments[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private Object defaultValue(Class<?> returnType) {
            if (returnType == Boolean.TYPE) {
                return false;
            }
            if (returnType == Optional.class) {
                return Optional.empty();
            }
            if (returnType == List.class) {
                return List.of();
            }
            return null;
        }
    }
}
