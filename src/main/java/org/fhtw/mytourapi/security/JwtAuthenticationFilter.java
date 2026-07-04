package org.fhtw.mytourapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.fhtw.mytourapi.domain.UserEntity;
import org.fhtw.mytourapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            ObjectProvider<UserRepository> userRepositoryProvider
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepositoryProvider.getIfAvailable();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid authorization header");
            return;
        }

        try {
            authenticate(authorization.substring(BEARER_PREFIX.length()));
            filterChain.doFilter(request, response);
        } catch (InvalidJwtException exception) {
            LOGGER.warn("Rejected invalid JWT at {} failure={}", request.getRequestURI(), exception.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token");
        }
    }

    private void authenticate(String token) {
        JwtClaims claims = jwtService.parse(token);
        if (userRepository == null) {
            throw new InvalidJwtException("User repository is not available.");
        }

        UserEntity user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new InvalidJwtException("JWT user no longer exists."));
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(user.getId(), user.getUsername());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
