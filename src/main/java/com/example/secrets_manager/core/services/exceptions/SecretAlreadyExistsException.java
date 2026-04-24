package com.example.secrets_manager.core.services.exceptions;

/** Exception thrown when an active secret with the same name already exists in a group. */
public class SecretAlreadyExistsException extends SecretServiceException {
  public SecretAlreadyExistsException(String message) {
    super(message);
  }

  public SecretAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
