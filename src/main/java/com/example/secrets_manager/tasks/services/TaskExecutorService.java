package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tracing.CorrelationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Orchestrator for asynchronous task execution. Submits tasks to the thread pool and dispatches
 * them to their respective handlers.
 */
@Service
@Slf4j
public class TaskExecutorService {

  private final TaskHandlerRegistry handlerRegistry;
  private final ThreadPoolTaskExecutor taskExecutor;

  @Autowired
  public TaskExecutorService(
      TaskHandlerRegistry handlerRegistry,
      @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor) {
    this.handlerRegistry = handlerRegistry;
    this.taskExecutor = taskExecutor;
  }

  /** Submits a task for asynchronous background execution. */
  public void submitTask(Task task) {
    // Wrap the background thread in the same Correlation Context as the initiator.
    // This ensures end-to-end traceability across thread boundaries.
    taskExecutor.execute(
        () -> CorrelationContext.runWithId(task.getCorrelationId(), () -> dispatchToHandler(task)));
  }

  /** Finds the correct handler and delegates the full execution lifecycle to it. */
  private void dispatchToHandler(Task task) {
    final var handler = handlerRegistry.getHandler(task.getType());
    if (handler.isPresent()) {
      handler.get().run(task);
    } else {
      log.error(
          "CRITICAL: No handler found for task type: {}. Task ID: {}",
          task.getType(),
          task.getId());
    }
  }
}
