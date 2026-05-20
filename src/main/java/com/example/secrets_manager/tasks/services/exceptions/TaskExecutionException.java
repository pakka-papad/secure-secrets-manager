package com.example.secrets_manager.tasks.services.exceptions;

import java.util.UUID;
import lombok.Getter;

/**
 * Thrown when a task handler decides to stop execution due to an unrecoverable domain-level failure
 * that doesn't fit into standard framework reasons like eviction or cancellation.
 */
@Getter
public class TaskExecutionException extends RuntimeException {
  private final UUID taskId;

  public TaskExecutionException(UUID taskId, String message) {
    super(message);
    this.taskId = taskId;
  }
}
