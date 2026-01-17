package com.example.secrets_manager.core.services.exceptions;

public class UserAlreadyExistsException extends UserServiceException {
  public UserAlreadyExistsException(String message) {
    super(message);
  }

  public UserAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
