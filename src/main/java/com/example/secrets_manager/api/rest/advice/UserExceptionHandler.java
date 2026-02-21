package com.example.secrets_manager.api.rest.advice;

import com.example.secrets_manager.api.rest.dto.ErrorResponse;
import com.example.secrets_manager.core.services.exceptions.InvalidPasswordException;
import com.example.secrets_manager.core.services.exceptions.TokenRevokedException;
import com.example.secrets_manager.core.services.exceptions.UserAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.UserServiceException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionHandler {

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(
      UserAlreadyExistsException ex, HttpServletRequest request) {
    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.CONFLICT.value()) // 409
            .error(HttpStatus.CONFLICT.getReasonPhrase())
            .messages(List.of(ex.getMessage()))
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(InvalidPasswordException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPasswordException(
      InvalidPasswordException ex, HttpServletRequest request) {
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

  @ExceptionHandler(TokenRevokedException.class)
  public ResponseEntity<ErrorResponse> handleTokenRevokedException(
      TokenRevokedException ex, HttpServletRequest request) {
    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.UNAUTHORIZED.value()) // 401
            .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
            .messages(List.of(ex.getMessage()))
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
  }

  @ExceptionHandler(UserServiceException.class)
  public ResponseEntity<ErrorResponse> handleUserServiceException(
      UserServiceException ex, HttpServletRequest request) {
    var errorResponse =
        ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
            .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
            .messages(List.of(ex.getMessage()))
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
