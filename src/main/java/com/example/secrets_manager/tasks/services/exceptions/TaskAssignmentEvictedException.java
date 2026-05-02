package com.example.secrets_manager.tasks.services.exceptions;

import java.util.UUID;

/**
 * Exception thrown when a worker attempts to persist task state but no longer holds the assignment.
 */
public class TaskAssignmentEvictedException extends RuntimeException {
  public TaskAssignmentEvictedException(UUID taskId) {
    super("Task assignment was lost/evicted for task " + taskId);
  }
}
