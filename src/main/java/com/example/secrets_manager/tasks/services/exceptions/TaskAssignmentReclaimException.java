package com.example.secrets_manager.tasks.services.exceptions;

import java.util.UUID;
import lombok.Getter;

/**
 * Thrown when a stale assignment was released but could not be replaced in the same transaction.
 */
@Getter
public class TaskAssignmentReclaimException extends RuntimeException {
  private final UUID taskId;
  private final UUID previousWorkerId;

  public TaskAssignmentReclaimException(UUID taskId, UUID previousWorkerId) {
    super(
        "Failed to replace assignment for task "
            + taskId
            + " after releasing stale worker "
            + previousWorkerId);
    this.taskId = taskId;
    this.previousWorkerId = previousWorkerId;
  }
}
