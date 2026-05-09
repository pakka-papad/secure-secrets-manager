package com.example.secrets_manager.tracing;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.MDC;

/**
 * Utility for managing the Correlation ID in a ThreadLocal context. This ensures that tracing
 * information is available throughout the execution of a single thread.
 *
 * <p>Integrates with SLF4J MDC to ensure that log patterns automatically include the ID.
 */
public class CorrelationContext {

  private static final ThreadLocal<UUID> CONTEXT = new ThreadLocal<>();
  private static final String MDC_KEY = "correlationId";

  /** Sets the correlation ID for the current thread and updates MDC. */
  public static void set(UUID correlationId) {
    CONTEXT.set(correlationId);
    if (correlationId != null) {
      MDC.put(MDC_KEY, correlationId.toString());
    } else {
      MDC.remove(MDC_KEY);
    }
  }

  /** Gets the correlation ID for the current thread. */
  public static Optional<UUID> get() {
    return Optional.ofNullable(CONTEXT.get());
  }

  /** Clears the correlation ID from the current thread and MDC. Important for thread-pool reuse. */
  public static void clear() {
    CONTEXT.remove();
    MDC.remove(MDC_KEY);
  }

  /**
   * Executes a task within a specific correlation context. Ensures the context is cleaned up
   * afterward.
   */
  public static void runWithId(UUID id, Runnable task) {
    try {
      set(id);
      task.run();
    } finally {
      clear();
    }
  }

  /**
   * Executes a supplier within a specific correlation context. Ensures the context is cleaned up
   * afterward.
   */
  public static <T> T supplyWithId(UUID id, Supplier<T> supplier) {
    try {
      set(id);
      return supplier.get();
    } finally {
      clear();
    }
  }
}
