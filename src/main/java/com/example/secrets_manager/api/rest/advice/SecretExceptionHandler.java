package com.example.secrets_manager.api.rest.advice;

import com.example.secrets_manager.api.rest.dto.ErrorResponse;
import com.example.secrets_manager.core.services.exceptions.SecretAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.SecretServiceException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Centralized exception handler for Secret related errors. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecretExceptionHandler {

  @ExceptionHandler(SecretAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleSecretAlreadyExistsException(
      SecretAlreadyExistsException ex, HttpServletRequest request) {
    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.CONFLICT.value())
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .messages(List.of(ex.getMessage()))
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(SecretServiceException.class)
  public ResponseEntity<ErrorResponse> handleSecretServiceException(
      SecretServiceException ex, HttpServletRequest request) {
    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .messages(List.of(ex.getMessage()))
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
