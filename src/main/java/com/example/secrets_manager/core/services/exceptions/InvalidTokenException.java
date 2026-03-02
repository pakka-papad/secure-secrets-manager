package com.example.secrets_manager.core.services.exceptions;

/**
 * Exception thrown when a provided token (JWT, refresh token, etc.) is cryptographically invalid,
 * expired, or malformed.
 */
public class InvalidTokenException extends RuntimeException {
  public InvalidTokenException(String message) {
    super(message);
  }

  public InvalidTokenException(String message, Throwable cause) {
    super(message, cause);
  }
}
