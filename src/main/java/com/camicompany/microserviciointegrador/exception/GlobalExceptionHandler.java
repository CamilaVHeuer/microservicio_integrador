package com.camicompany.microserviciointegrador.exception;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ApiError> handleExternalServiceException(ExternalServiceException ex) {
    ApiError error =
        new ApiError(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            HttpStatus.SERVICE_UNAVAILABLE.name(),
            ex.getMessage(),
            LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
  }

  @ExceptionHandler(ExternalServiceBadRequestException.class)
  public ResponseEntity<ApiError> handleExternalServiceBadRequest(
      ExternalServiceBadRequestException ex) {
    ApiError error =
        new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.name(),
            ex.getMessage(),
            LocalDateTime.now());
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
    ApiError error =
        new ApiError(
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.name(),
            ex.getMessage(),
            LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(StatusConflictException.class)
  public ResponseEntity<ApiError> handleStatusConflict(StatusConflictException ex) {
    ApiError error =
        new ApiError(
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.name(),
            ex.getMessage(),
            LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(InvalidCredentialsException ex) {
    ApiError error =
        new ApiError(
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.name(),
            ex.getMessage(),
            LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(InternalServiceException.class)
  public ResponseEntity<ApiError> handleBadCredentials(InternalServiceException ex) {
    ApiError error =
        new ApiError(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.name(),
            ex.getMessage(),
            LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {

    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .findFirst()
            .orElse("Validation error");

    ApiError error =
        new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.name(),
            message,
            LocalDateTime.now());
    return ResponseEntity.badRequest().body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGenericException(Exception ex) {

    ApiError error =
        new ApiError(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.name(),
            "Unexpected error",
            LocalDateTime.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
