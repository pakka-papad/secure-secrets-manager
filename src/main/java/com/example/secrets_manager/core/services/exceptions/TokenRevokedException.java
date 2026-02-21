package com.example.secrets_manager.core.services.exceptions;

/**
 * Thrown when a token is cryptographically valid but has been explicitly revoked or invalidated in
 * the system.
 */
public class TokenRevokedException extends RuntimeException {
  public TokenRevokedException(String message) {
    super(message);
  }
}
