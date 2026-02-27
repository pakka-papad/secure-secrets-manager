package com.example.secrets_manager.api.rest.advice;

import com.example.secrets_manager.api.rest.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AppExceptionHandler {

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
      EntityNotFoundException ex, HttpServletRequest request) {
    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value()) // 404
            .error(HttpStatus.NOT_FOUND.getReasonPhrase())
            .messages(List.of(ex.getMessage()))
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(
      AccessDeniedException ex, HttpServletRequest request) {
    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value()) // 403
            .error(HttpStatus.FORBIDDEN.getReasonPhrase())
            .messages(List.of(ex.getMessage()))
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(PessimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handlePessimisticLockingFailureException(
      PessimisticLockingFailureException ex, HttpServletRequest request) {
    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.SERVICE_UNAVAILABLE.value()) // 503
            .error(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
            .messages(
                List.of(
                    "The system is currently busy processing other requests for this resource. Please try again in a few moments."))
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    // Collect all validation error messages into a list
    List<String> errorMessages =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(java.util.stream.Collectors.toList());

    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value()) // 400
            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
            .messages(errorMessages)
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  // Generic exception handler for any other unhandled exceptions
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleAllExceptions(
      Exception ex, HttpServletRequest request) {
    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .messages(List.of("An unexpected error occurred: " + ex.getMessage()))
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
