package org.fhtw.mytourapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.fhtw.mytourapi.dto.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Stream;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String VALIDATION_FAILED = "Validation failed";

    private final ApiErrorResponseFactory errorResponseFactory;

    public ApiExceptionHandler(ApiErrorResponseFactory errorResponseFactory) {
        this.errorResponseFactory = errorResponseFactory;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();
        if (statusCode.is5xxServerError()) {
            LOGGER.error("Response status exception at {} status={}", request.getRequestURI(), statusCode, exception);
        } else {
            LOGGER.debug("Response status exception at {} status={}", request.getRequestURI(), statusCode);
        }

        return errorResponseFactory.create(
                statusCode,
                messageOrDefault(exception.getReason(), errorResponseFactory.reasonPhrase(statusCode)),
                request,
                List.of()
        );
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleUpstreamService(
            UpstreamServiceException exception,
            HttpServletRequest request
    ) {
        LOGGER.warn(
                "Upstream service exception at {} status={} failureClass={}",
                request.getRequestURI(),
                exception.status(),
                exception.getCause() == null ? exception.getClass().getSimpleName() : exception.getCause().getClass().getSimpleName()
        );

        return errorResponseFactory.create(
                exception.status(),
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleFileStorage(
            FileStorageException exception,
            HttpServletRequest request
    ) {
        if (exception.status().is5xxServerError()) {
            LOGGER.error("File storage exception at {}", request.getRequestURI(), exception);
        }

        return errorResponseFactory.create(
                exception.status(),
                exception.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(ImportValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleImportValidation(
            ImportValidationException exception,
            HttpServletRequest request
    ) {
        LOGGER.debug("Import validation exception at {} validationErrorCount={}", request.getRequestURI(), exception.validationErrors().size());
        return errorResponseFactory.create(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request,
                exception.validationErrors()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<String> validationErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::formatFieldError)
                .toList();

        List<String> globalErrors = exception.getBindingResult().getGlobalErrors().stream()
                .map(ApiExceptionHandler::formatObjectError)
                .toList();

        LOGGER.debug(
                "Request body validation failed at {} validationErrorCount={}",
                request.getRequestURI(),
                validationErrors.size() + globalErrors.size()
        );
        return errorResponseFactory.create(
                HttpStatus.BAD_REQUEST,
                VALIDATION_FAILED,
                request,
                merge(validationErrors, globalErrors)
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        List<String> validationErrors = exception.getParameterValidationResults().stream()
                .flatMap((result) -> result.getResolvableErrors().stream()
                        .map((error) -> parameterName(result) + ": "
                                + messageOrDefault(error.getDefaultMessage(), "Invalid value")))
                .sorted()
                .toList();

        LOGGER.debug("Handler method validation failed at {} validationErrorCount={}", request.getRequestURI(), validationErrors.size());
        return errorResponseFactory.create(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, request, validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<String> validationErrors = exception.getConstraintViolations().stream()
                .map(ApiExceptionHandler::formatConstraintViolation)
                .sorted()
                .toList();

        LOGGER.debug("Constraint validation failed at {} validationErrorCount={}", request.getRequestURI(), validationErrors.size());
        return errorResponseFactory.create(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, request, validationErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        LOGGER.debug("Malformed API request at {} failureClass={}", request.getRequestURI(), exception.getClass().getSimpleName());
        return errorResponseFactory.create(HttpStatus.BAD_REQUEST, badRequestMessage(exception), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled API exception at {}", request.getRequestURI(), exception);
        return errorResponseFactory.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request,
                List.of()
        );
    }

    private static String badRequestMessage(Exception exception) {
        if (exception instanceof MethodArgumentTypeMismatchException mismatchException) {
            return "Invalid value for '" + mismatchException.getName() + "'.";
        }

        if (exception instanceof MissingServletRequestParameterException missingParameterException) {
            return "Missing required parameter '" + missingParameterException.getParameterName() + "'.";
        }

        return "Malformed request body.";
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + ": " + messageOrDefault(error.getDefaultMessage(), "Invalid value");
    }

    private static String formatObjectError(ObjectError error) {
        return error.getObjectName() + ": " + messageOrDefault(error.getDefaultMessage(), "Invalid value");
    }

    private static String formatConstraintViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    private static String parameterName(ParameterValidationResult result) {
        String parameterName = result.getMethodParameter().getParameterName();
        return parameterName == null ? "parameter" : parameterName;
    }

    private static String messageOrDefault(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    private static List<String> merge(List<String> first, List<String> second) {
        if (first.isEmpty()) {
            return second;
        }

        if (second.isEmpty()) {
            return first;
        }

        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
