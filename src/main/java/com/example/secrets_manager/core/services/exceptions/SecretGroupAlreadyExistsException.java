package com.example.secrets_manager.core.services.exceptions;

/**
 * Thrown when an operation attempts to create a secret group with a name that already exists and is
 * active.
 */
public class SecretGroupAlreadyExistsException extends SecretGroupServiceException {
  public SecretGroupAlreadyExistsException(String message) {
    super(message);
  }

  public SecretGroupAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
