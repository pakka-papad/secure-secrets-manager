package com.example.secrets_manager.tasks.services.exceptions;

import java.util.UUID;
import lombok.Getter;

/**
 * Thrown when a fenced database update is rejected by the database (returns 0 rows) even though the
 * visible distributed fences (ownership and state) appear to be valid. Indicates a potential data
 * integrity paradox or unhandled constraint violation.
 */
@Getter
public class TaskFencedUpdateFailedException extends RuntimeException {
  private final UUID taskId;

  public TaskFencedUpdateFailedException(UUID taskId, String message) {
    super(message);
    this.taskId = taskId;
  }
}
