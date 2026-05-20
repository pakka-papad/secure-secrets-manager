package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.*;
import com.example.secrets_manager.tasks.models.events.TaskStartedEvent;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import com.example.secrets_manager.tasks.services.exceptions.TaskCancelledException;
import com.example.secrets_manager.tasks.services.exceptions.TaskFencedUpdateFailedException;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Specialized service for managing the atomic, transactional state transitions of background tasks.
 * Centralizing this logic allows TaskHandlers to remain non-proxied and maintain 'final' modifier
 * safety.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskExecutionOrchestrator {

  private final TaskRepository taskRepository;
  private final TaskAssignmentService assignmentService;
  private final TaskEntityConverter taskConverter;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Initializes the task to RUNNING state, persists the change with fencing, and executes the
   * provided pre-execute hook.
   */
  @Transactional
  public void startTask(Task task, Runnable preExecuteHook) {
    eventPublisher.publishEvent(new TaskStartedEvent(task.getId()));

    task.setState(TaskState.RUNNING);
    task.setStartedAt(Instant.now());

    persistStateWithFencing(task);

    preExecuteHook.run();
  }

  /**
   * Marks the task as COMPLETED, records the output, persists the change with fencing, and executes
   * the success hook.
   */
  @Transactional
  public <O extends TaskOutput> void completeTask(Task task, O output, Consumer<O> successHook) {
    task.setState(TaskState.COMPLETED);
    task.setCompletedAt(Instant.now());
    task.setOutput(output);

    persistStateWithFencing(task);

    successHook.accept(output);
  }

  /**
   * Marks the task as FAILED, persists the change with fencing, and executes the failure hook.
   * Handles eviction and cancellation cases gracefully.
   */
  @Transactional
  public void failTask(Task task, Exception e, Consumer<Exception> failureHook) {
    // Only skip persistence for Hard Fence Violations (where we no longer own the task)
    if (e instanceof TaskAssignmentEvictedException
        || e instanceof TaskCancelledException
        || e instanceof TaskFencedUpdateFailedException) {
      log.error(
          "Task {} failed due to hard fence violation (eviction, cancellation, or integrity failure). Skipping state persistence.",
          task.getId());
      return;
    }

    // For all other unrecoverable failures (including TaskExecutionException),
    // mark the task as FAILED and capture the reason in metadata.
    task.setState(TaskState.FAILED);
    task.setCompletedAt(Instant.now());
    task.setMetadata(String.format("{\"error\": \"%s\"}", e.getMessage()));

    try {
      persistStateWithFencing(task);
      failureHook.accept(e);
    } catch (TaskAssignmentEvictedException
        | TaskCancelledException
        | TaskFencedUpdateFailedException fenceEx) {
      log.warn(
          "Hard fence violation while attempting to persist FAILED state for task {}. Aborting failure hooks.",
          task.getId());
    } catch (Exception dbEx) {
      log.error("Database error while persisting failure for task {}.", task.getId(), dbEx);
    }
  }

  /** Updates the task's extra info (progress) and persists with fencing. */
  @Transactional
  public <E extends TaskStateExtraInfo> void updateProgress(Task task, E progressInfo) {
    task.setStateExtraInfo(progressInfo);
    persistStateWithFencing(task);
  }

  /**
   * Externally signals a task to cancel. This is an atomic operation that moves the task directly
   * to CANCELLED state if it is not already terminal.
   */
  @Transactional
  public void cancelTaskExternally(UUID taskId) {
    int updated = taskRepository.signalCancellation(taskId);
    if (updated == 1) {
      log.info("Task {} was successfully cancelled externally.", taskId);
    } else {
      log.warn("Task {} could not be cancelled (likely already terminal).", taskId);
    }
  }

  /**
   * Performs an atomic save-and-verify against the database. Throws TaskAssignmentEvictedException,
   * TaskCancelledException, or TaskFencedUpdateFailedException if the worker no longer owns the
   * task, it was cancelled, or an integrity error occurred.
   */
  public void persistStateWithFencing(Task task) {
    var entity = taskConverter.fromModel(task);

    int updated =
        taskRepository.updateFenced(
            entity.getId(),
            TaskUtils.WORKER_ID,
            entity.getState(),
            entity.getStartedAt(),
            entity.getCompletedAt(),
            entity.getTaskOutput(),
            entity.getStateExtraInfo(),
            entity.getMetadata());

    if (updated == 0) {
      verifyFence(task.getId());
    }
  }

  /**
   * Post-mortem check to identify why a fenced update failed.
   *
   * @param taskId The ID of the task to verify.
   * @throws TaskCancelledException if the task was cancelled.
   * @throws TaskAssignmentEvictedException if the node lost its assignment.
   * @throws TaskFencedUpdateFailedException if no visible fence was violated but the update still
   *     failed.
   */
  public void verifyFence(UUID taskId) {
    var current = taskRepository.findById(taskId).orElse(null);
    if (current == null) {
      throw new EntityNotFoundException("Task was deleted: " + taskId);
    }

    // State (Cancellation)
    if (TaskState.CANCELLED.name().equals(current.getState())) {
      throw new TaskCancelledException(taskId);
    }

    // Ownership (Eviction)
    if (!assignmentService.isAssignmentStillValid(taskId)) {
      throw new TaskAssignmentEvictedException(taskId);
    }

    // Fallback: Potential Paradox or Integrity Error
    throw new TaskFencedUpdateFailedException(
        taskId, "Fenced update rejected by database despite valid state and assignment.");
  }
}
