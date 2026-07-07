package org.fhtw.mytourapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.fhtw.mytourapi.dto.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler(new ApiErrorResponseFactory());
        request = new MockHttpServletRequest("POST", "/api/tours");
    }

    @Test
    void handleConflictReturns409() {
        ResponseEntity<ApiErrorResponse> response = handler.handleConflict(
                new ConflictException("Duplicate name"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).isEqualTo("Duplicate name");
        assertThat(response.getBody().path()).isEqualTo("/api/tours");
    }

    @Test
    void handleUnauthorizedReturns401() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnauthorized(
                new UnauthorizedException("Invalid credentials"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleResponseStatusReturns404ForNotFound() {
        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Tour not found");
    }

    @Test
    void handleResponseStatusUsesReasonPhraseWhenReasonIsNull() {
        ResponseEntity<ApiErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, null), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Not Found");
    }

    @Test
    void handleUpstreamServiceReturnsUpstreamStatus() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUpstreamService(
                new UpstreamServiceException(HttpStatus.BAD_GATEWAY, "Upstream failed"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(502);
        assertThat(response.getBody().message()).isEqualTo("Upstream failed");
    }

    @Test
    void handleFileStorageReturns400ForBadRequest() {
        ResponseEntity<ApiErrorResponse> response = handler.handleFileStorage(
                FileStorageException.badRequest("Invalid path"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Invalid path");
    }

    @Test
    void handleFileStorageReturns500ForInternalError() {
        ResponseEntity<ApiErrorResponse> response = handler.handleFileStorage(
                FileStorageException.internal("Disk failure", new RuntimeException("io")), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
    }

    @Test
    void handleImportValidationReturns400WithValidationErrors() {
        ImportValidationException exception = new ImportValidationException(
                List.of("tours[0].route: must not be null", "tours[0].plannedDistanceM: must not be null")
        );

        ResponseEntity<ApiErrorResponse> response = handler.handleImportValidation(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().validationErrors()).hasSize(2);
        assertThat(response.getBody().validationErrors())
                .contains("tours[0].route: must not be null");
    }

    @Test
    void handleUnexpectedReturns500() {
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("Unexpected error"), request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred.");
        assertThat(response.getBody().validationErrors()).isEmpty();
    }
}
