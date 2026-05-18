package com.example.secrets_manager.tasks.services.exceptions;

import java.util.UUID;
import lombok.Getter;

/** Thrown when a task execution is interrupted by an external cancellation signal. */
@Getter
public class TaskCancelledException extends RuntimeException {
  private final UUID taskId;

  public TaskCancelledException(UUID taskId) {
    super("Task " + taskId + " has been cancelled.");
    this.taskId = taskId;
  }
}
