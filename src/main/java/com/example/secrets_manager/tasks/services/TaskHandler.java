package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tasks.models.TaskInput;
import com.example.secrets_manager.tasks.models.TaskOutput;
import com.example.secrets_manager.tasks.models.TaskType;

/**
 * Interface for components that implement the logic for a specific task type.
 *
 * @param <I> The input payload type.
 * @param <O> The output payload type.
 */
public interface TaskHandler<I extends TaskInput, O extends TaskOutput> {

  /** Returns the type of task this handler can process. */
  TaskType getSupportedType();

  /** Executes the core task logic. */
  O execute(I input) throws Exception;

  // Lifecycle Hooks (Internal methods used by the engine to wrap logic)
  void runPreExecute(Task task);

  void runPostExecuteSuccess(Task task, O output);

  void runPostExecuteFailure(Task task, Exception e);

  void runCleanup(Task task);
}
