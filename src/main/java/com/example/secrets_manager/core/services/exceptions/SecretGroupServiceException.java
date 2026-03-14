package com.example.secrets_manager.core.services.exceptions;

/** Base exception for all errors originating from the Secret Group Service. */
public class SecretGroupServiceException extends RuntimeException {
  public SecretGroupServiceException(String message) {
    super(message);
  }

  public SecretGroupServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
