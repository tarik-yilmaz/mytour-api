package org.fhtw.mytourapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.fhtw.mytourapi.dto.AuthResponse;
import org.fhtw.mytourapi.dto.LoginRequest;
import org.fhtw.mytourapi.dto.RegisterRequest;
import org.fhtw.mytourapi.dto.UserDto;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Username/password registration and JWT login.")
public class AuthController {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new local user and return a JWT.")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return notImplemented();
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with username/password and return a JWT.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return notImplemented();
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user's public profile.")
    public UserDto currentUser() {
        return notImplemented();
    }

    private static <T> T notImplemented() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Authentication service is not implemented yet");
    }
}
