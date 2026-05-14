package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.*;
import com.example.secrets_manager.tasks.models.events.TaskStartedEvent;
import com.example.secrets_manager.tasks.models.events.TaskStoppedEvent;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base for TaskHandlers using the "Strict Template Method" pattern with Fencing and
 * Events. Manages type-safe execution and lifecycle transitions.
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

  protected final TaskRepository taskRepository;
  protected final TaskAssignmentService assignmentService;
  protected final TaskEntityConverter taskConverter;
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
   * @throws RuntimeException for other failure reasons.
   */
  protected final void abort(AbortReason reason, UUID taskId) {
    // Immediate silence: remove from local registry to stop the heartbeat
    eventPublisher.publishEvent(new TaskStoppedEvent(taskId));

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
      runPreExecute(task);

      // Distributed Guard: Verify we still own it before entering core logic
      if (!assignmentService.isAssignmentStillValid(task.getId())) {
        log.warn("Lost assignment for task {}. Aborting logic.", task.getId());
        return;
      }

      // Core Logic: Delegate to subclass with an explicit, functional context channel
      final var context = createTaskContext(task);
      final var output = execute(context);

      // Framework Success: Persist final output and mark COMPLETED
      runPostExecuteSuccess(task, output);

    } catch (Exception e) {
      log.error("Execution failed for task {}", task.getId(), e);
      // Framework Failure: Persist error state and mark FAILED
      runPostExecuteFailure(task, e);
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

  @Transactional
  protected final void runPreExecute(Task task) {
    // Register in local registry via Event
    eventPublisher.publishEvent(new TaskStartedEvent(task.getId()));

    // Mandatory Framework Logic: Initialize start time and state
    task.setState(TaskState.RUNNING);
    task.setStartedAt(Instant.now());

    persistStateWithFencing(task);

    // Delegate to optional subclass hook
    onPreExecute(task);
  }

  @Transactional
  protected final void runPostExecuteSuccess(Task task, O output) {
    // Mandatory Framework Logic: Record output and completion time
    task.setState(TaskState.COMPLETED);
    task.setCompletedAt(Instant.now());
    task.setOutput(output);

    persistStateWithFencing(task);

    // Delegate to optional subclass hook
    onPostExecuteSuccess(task, output);
  }

  @Transactional
  protected final void runPostExecuteFailure(Task task, Exception originalException) {
    // If the original failure was already an eviction, we abort immediately
    if (originalException instanceof TaskAssignmentEvictedException) {
      log.error("Task {} failed due to eviction. Aborting all logic.", task.getId());
      return;
    }

    // Mandatory Framework Logic: Try to persist the FAILED state
    task.setState(TaskState.FAILED);
    task.setCompletedAt(Instant.now());

    try {
      persistStateWithFencing(task);
    } catch (TaskAssignmentEvictedException evictedEx) {
      // If we get evicted while reporting a failure, we must stop.
      // Another node may have already taken over or finished the task.
      log.warn(
          "Evicted while attempting to persist FAILED state for task {}. Aborting failure hooks.",
          task.getId());
      return;
    } catch (Exception dbEx) {
      log.error("Database error while persisting failure for task {}.", task.getId(), dbEx);
    }

    // Delegate to optional subclass hook (only if not evicted)
    onPostExecuteFailure(task, originalException);
  }

  protected final void runCleanup(Task task) {
    // Stage 1: Unregister locally (Silent the heartbeat immediately)
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

  /**
   * Performs an atomic save-and-verify against the database. Throws TaskAssignmentEvictedException
   * if the worker no longer owns the task.
   */
  protected void persistStateWithFencing(Task task) {
    var entity = taskConverter.fromModel(task);

    int updated =
        taskRepository.updateFenced(
            entity.getId(),
            TaskUtils.WORKER_ID,
            entity.getState(),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            entity.getTaskOutput(),
            entity.getStateExtraInfo());

    if (updated == 0) {
      abort(AbortReason.EVICTED, entity.getId());
    }
  }

  /** Creates a synchronous progress-reporting channel for the given task. */
  @SuppressWarnings("unchecked")
  protected final TaskContext<I, E> createTaskContext(Task task) {
    return new TaskContext<>(
        task.getId(),
        (I) task.getInput(),
        progressInfo -> {
          task.setStateExtraInfo(progressInfo);
          persistStateWithFencing(task);
        });
  }

  // --- Protected Hooks (Optional override by subclasses) ---

  protected void onPreExecute(Task task) {}

  protected void onPostExecuteSuccess(Task task, O output) {}

  protected void onPostExecuteFailure(Task task, Exception e) {}

  protected void onCleanup(Task task) {}
}
