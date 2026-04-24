package com.example.secrets_manager.core.services.exceptions;

/** Base exception for all errors originating from the Secret Service. */
public class SecretServiceException extends RuntimeException {
  public SecretServiceException(String message) {
    super(message);
  }

  public SecretServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
