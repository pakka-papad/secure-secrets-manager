package com.example.secrets_manager.core.services.exceptions;

public class InvalidPasswordException extends UserServiceException {
  public InvalidPasswordException(String message) {
    super(message);
  }

  public InvalidPasswordException(String message, Throwable cause) {
    super(message, cause);
  }
}
