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

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String VALIDATION_FAILED = "Validation failed";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();
        return buildResponse(
                statusCode,
                messageOrDefault(exception.getReason(), reasonPhrase(statusCode)),
                request,
                List.of()
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

        return buildResponse(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, request, merge(validationErrors, globalErrors));
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

        return buildResponse(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, request, validationErrors);
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

        return buildResponse(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, request, validationErrors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, badRequestMessage(exception), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled API exception at {}", request.getRequestURI(), exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.",
                request,
                List.of()
        );
    }

    private static ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatusCode statusCode,
            String message,
            HttpServletRequest request,
            List<String> validationErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                statusCode.value(),
                reasonPhrase(statusCode),
                message,
                request.getRequestURI(),
                validationErrors
        );

        return ResponseEntity.status(statusCode).body(response);
    }

    private static String reasonPhrase(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        return status == null ? "HTTP " + statusCode.value() : status.getReasonPhrase();
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
