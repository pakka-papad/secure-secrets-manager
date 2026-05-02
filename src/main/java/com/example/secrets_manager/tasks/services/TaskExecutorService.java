package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.models.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Orchestrator for asynchronous task execution. Submits tasks to the thread pool and delegates to
 * the decentralized lifecycle handlers.
 */
@Service
@Slf4j
public class TaskExecutorService {

  private final TaskHandlerRegistry handlerRegistry;
  private final TaskAssignmentService assignmentService;
  private final ThreadPoolTaskExecutor taskExecutor;

  @Autowired
  public TaskExecutorService(
      TaskHandlerRegistry handlerRegistry,
      TaskAssignmentService assignmentService,
      @Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor) {
    this.handlerRegistry = handlerRegistry;
    this.assignmentService = assignmentService;
    this.taskExecutor = taskExecutor;
  }

  /** Submits a task for asynchronous background execution. */
  public void submitTask(Task task) {
    taskExecutor.execute(() -> executeDecentralizedLifecycle(task));
  }

  /** Orchestrates the execution sequence using the decentralized handler logic. */
  private void executeDecentralizedLifecycle(Task task) {
    var handler =
        handlerRegistry
            .getHandler(task.getType())
            .orElseThrow(() -> new IllegalStateException("No handler for " + task.getType()));

    try {
      // 1. Framework Pre-Execute (Heartbeat, State Change)
      handler.runPreExecute(task);

      // Distributed Guard: Verify we still own it
      if (!assignmentService.isAssignmentStillValid(task.getId())) {
        log.warn("Lost assignment for task {}. Aborting logic.", task.getId());
        return;
      }

      // 2. Core Logic
      var output = handler.execute(task.getInput());

      // 3. Framework Success (Mark Completed)
      handler.runPostExecuteSuccess(task, output);

    } catch (Exception e) {
      log.error("Execution failed for task {}", task.getId(), e);
      // 4. Framework Failure (Mark Failed)
      handler.runPostExecuteFailure(task, e);
    } finally {
      // 5. Framework Cleanup (Release Assignment)
      handler.runCleanup(task);
    }
  }
}
