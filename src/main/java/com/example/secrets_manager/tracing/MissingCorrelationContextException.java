package com.example.secrets_manager.tracing;

/**
 * Exception thrown when a mandatory Correlation ID is missing from the current execution context.
 * This is used to prevent the persistence of audit or security logs that lack traceability.
 */
public class MissingCorrelationContextException extends RuntimeException {
  public MissingCorrelationContextException(String message) {
    super(message);
  }
}
