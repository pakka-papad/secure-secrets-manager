package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tasks.models.TaskType;

/**
 * Entry-point interface for components that process background tasks. Acts as a non-generic
 * dispatcher contract for the execution engine.
 */
public interface TaskHandler {

  /** Returns the type of task this handler can process. */
  TaskType getSupportedType();

  /**
   * The entry point called by the execution engine to perform the full task lifecycle. Handles
   * pre-execution setup, core logic execution, post-execution state updates, and cleanup.
   *
   * @param task The task model to be executed.
   */
  void run(Task task);
}
