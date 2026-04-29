package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.models.TaskInput;
import com.example.secrets_manager.tasks.models.TaskOutput;
import com.example.secrets_manager.tasks.models.TaskType;

/**
 * Interface for components that implement the actual logic for a specific task type.
 *
 * @param <I> The input payload type.
 * @param <O> The output payload type.
 */
public interface TaskHandler<I extends TaskInput, O extends TaskOutput> {

  /** Returns the type of task this handler can process. */
  TaskType getSupportedType();

  /**
   * Executes the task logic.
   *
   * @param input The typed input payload.
   * @return The typed output payload representing the result.
   */
  O execute(I input);
}
