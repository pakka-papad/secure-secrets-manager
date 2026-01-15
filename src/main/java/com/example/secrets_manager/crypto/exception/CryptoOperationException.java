package com.example.secrets_manager.crypto.exception;

/**
 * A checked exception thrown when a cryptographic operation (like decryption) fails due to
 * operational reasons, such as an incorrect key, tampered/corrupt data, or invalid padding.
 *
 * <p>This is distinct from runtime exceptions that may occur due to unrecoverable configuration or
 * programming errors (e.g., a required algorithm not being available).
 */
public class CryptoOperationException extends Exception {

  public CryptoOperationException(String message, Throwable cause) {
    super(message, cause);
  }
}
