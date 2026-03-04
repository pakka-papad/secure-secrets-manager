package com.example.secrets_manager.core.services.exceptions;

/**
 * Exception thrown when an operation would result in the system having no active administrators
 * (deadlock prevention).
 */
public class AdminDemotionException extends UserServiceException {
  public AdminDemotionException(String message) {
    super(message);
  }
}
