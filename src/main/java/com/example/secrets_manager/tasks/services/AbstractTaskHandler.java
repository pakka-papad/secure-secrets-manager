package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.models.*;
import com.example.secrets_manager.tasks.models.events.TaskStoppedEvent;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Abstract base for TaskHandlers using the "Strict Template Method" pattern. Manages type-safe
 * execution and lifecycle transitions by delegating transactional persistence to the {@link
 * TaskExecutionOrchestrator}.
 *
 * @param <I> The input payload type.
 * @param <O> The output payload type.
 * @param <E> The progress update (extra info) type.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTaskHandler<
        I extends TaskInput, O extends TaskOutput, E extends TaskStateExtraInfo>
    implements TaskHandler {

  protected final TaskExecutionOrchestrator orchestrator;
  protected final TaskAssignmentService assignmentService;
  protected final ApplicationEventPublisher eventPublisher;

  /** Standard reasons for manually aborting a task lifecycle. */
  protected enum AbortReason {
    /** The worker lost its assignment (evicted by another node or heartbeat timeout). */
    EVICTED
  }

  /**
   * Standardized way for subclasses to immediately halt task execution.
   *
   * @param reason The reason for the abort.
   * @param taskId The ID of the task being aborted.
   * @throws TaskAssignmentEvictedException if reason is EVICTED.
   */
  protected final void abort(AbortReason reason, UUID taskId) {
    if (reason == AbortReason.EVICTED) {
      throw new TaskAssignmentEvictedException(taskId);
    }
  }

  /**
   * Implementation of the core lifecycle orchestration. Orchestrates the transition from PENDING to
   * RUNNING, execution of business logic, and final state persistence.
   */
  @Override
  public final void run(Task task) {
    try {
      // Framework Pre-Execute (Heartbeat, State Change)
      orchestrator.startTask(task, () -> onPreExecute(task));

      // Distributed Guard: Verify we still own it before entering core logic
      if (!assignmentService.isAssignmentStillValid(task.getId())) {
        log.warn("Lost assignment for task {}. Aborting logic.", task.getId());
        return;
      }

      // Core Logic: Delegate to subclass with an explicit, functional context channel
      final var context = createTaskContext(task);
      final var output = execute(context);

      // Framework Success: Persist final output and mark COMPLETED
      orchestrator.completeTask(task, output, res -> onPostExecuteSuccess(task, res));

    } catch (Exception e) {
      log.error("Execution failed for task {}", task.getId(), e);
      // Framework Failure: Persist error state and mark FAILED
      orchestrator.failTask(task, e, ex -> onPostExecuteFailure(task, ex));
    } finally {
      // Framework Cleanup: Release local and remote assignments
      runCleanup(task);
    }
  }

  /**
   * The core business logic to be implemented by specific task handlers.
   *
   * @param context The context providing access to task metadata and progress reporting.
   * @return The final output of the task.
   */
  protected abstract O execute(TaskContext<I, E> context) throws Exception;

  // --- Internal Framework Lifecycle Logic ---

  protected final void runCleanup(Task task) {
    // Stage 1: Unregister locally (Silence the heartbeat immediately)
    try {
      eventPublisher.publishEvent(new TaskStoppedEvent(task.getId()));
    } catch (Exception e) {
      log.error("Failed to publish TaskStoppedEvent for {}", task.getId(), e);
    }

    // Stage 2: Release DB assignment
    try {
      assignmentService.releaseTask(task.getId());
    } catch (Exception e) {
      log.error("Failed to release assignment for {}", task.getId(), e);
    }

    // Stage 3: Subclass hook
    try {
      onCleanup(task);
    } catch (Exception e) {
      log.error("Failed subclass cleanup hook for {}", task.getId(), e);
    }
  }

  /** Creates a synchronous progress-reporting channel for the given task. */
  @SuppressWarnings("unchecked")
  protected final TaskContext<I, E> createTaskContext(Task task) {
    return new TaskContext<>(
        task.getId(),
        (I) task.getInput(),
        progressInfo -> orchestrator.updateProgress(task, progressInfo));
  }

  // --- Protected Hooks (Optional override by subclasses) ---

  protected void onPreExecute(Task task) {}

  protected void onPostExecuteSuccess(Task task, O output) {}

  protected void onPostExecuteFailure(Task task, Exception e) {}

  protected void onCleanup(Task task) {}
}
