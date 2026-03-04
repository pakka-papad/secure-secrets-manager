package com.example.secrets_manager.core.services.exceptions;

/**
 * Exception thrown when an administrator attempts to modify their own roles. This is prohibited to
 * ensure separation of duties and prevent accidental self-lockout.
 */
public class SelfDemotionException extends UserServiceException {
  public SelfDemotionException(String message) {
    super(message);
  }
}
