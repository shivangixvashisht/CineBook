package com.cinebook.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "Please correct the highlighted fields.", errors);
    }

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(MovieNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidMovieIdException.class)
    public ResponseEntity<ApiError> handleInvalidId(InvalidMovieIdException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of("movieId", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Movie ID must be a positive whole number.";
        return build(HttpStatus.BAD_REQUEST, msg, Map.of("movieId", msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed. Ticket price must be a number.", Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type must be application/json.", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "This operation is not supported.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Constraint violation", ex);
        return build(HttpStatus.BAD_REQUEST, "The data violates a database constraint.", Map.of());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccess(DataAccessException ex) {
        log.error("Database error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred. Please try again.", Map.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, Map<String, String> errors) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message, errors));
    }
}
