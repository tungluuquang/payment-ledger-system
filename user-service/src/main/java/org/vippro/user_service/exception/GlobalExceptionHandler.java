package org.vippro.user_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> handleConflict(ConflictException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return response(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                )
        );

        return response(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                errors
        );
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status).body(
                new ApiError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        fieldErrors
                )
        );
    }
}
