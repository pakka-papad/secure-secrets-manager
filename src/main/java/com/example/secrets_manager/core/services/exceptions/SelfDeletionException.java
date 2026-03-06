package com.example.secrets_manager.core.services.exceptions;

/**
 * Exception thrown when an administrator attempts to delete their own account. This is prohibited
 * to ensure governance and prevent accidental loss of all admin accounts.
 */
public class SelfDeletionException extends UserServiceException {
  public SelfDeletionException(String message) {
    super(message);
  }
}
