package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.*;
import com.example.secrets_manager.tasks.models.events.TaskStartedEvent;
import com.example.secrets_manager.tasks.models.events.TaskStoppedEvent;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base for TaskHandlers using the "Strict Template Method" pattern with Fencing and
 * Events.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTaskHandler<I extends TaskInput, O extends TaskOutput>
    implements TaskHandler<I, O> {

  protected final TaskRepository taskRepository;
  protected final TaskAssignmentService assignmentService;
  protected final TaskEntityConverter taskConverter;
  protected final ApplicationEventPublisher eventPublisher;

  // --- Final Template Methods (Framework Core) ---

  @Override
  @Transactional
  public final void runPreExecute(Task task) {
    // 1. Register in local registry via Event
    eventPublisher.publishEvent(new TaskStartedEvent(task.getId()));

    // 2. Mandatory Framework Logic (State Update)
    task.setState(TaskState.RUNNING);
    task.setStartedAt(Instant.now());

    persistStateWithFencing(task);

    // 3. Delegate to optional subclass hook
    onPreExecute(task);
  }

  @Override
  @Transactional
  public final void runPostExecuteSuccess(Task task, O output) {
    // 1. Mandatory Framework Logic
    task.setState(TaskState.COMPLETED);
    task.setCompletedAt(Instant.now());
    task.setOutput(output);

    persistStateWithFencing(task);

    // 2. Delegate to optional subclass hook
    onPostExecuteSuccess(task, output);
  }

  @Override
  @Transactional
  public final void runPostExecuteFailure(Task task, Exception originalException) {
    // 1. If the original failure was already an eviction, we abort immediately
    if (originalException instanceof TaskAssignmentEvictedException) {
      log.error("Task {} failed due to eviction. Aborting all logic.", task.getId());
      return;
    }

    // 2. Mandatory Framework Logic: Try to persist the FAILED state
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

    // 3. Delegate to optional subclass hook
    // ONLY if we were not evicted (meaning the persistStateWithFencing call succeeded)
    onPostExecuteFailure(task, originalException);
  }

  @Override
  public final void runCleanup(Task task) {
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
   * Performs an atomic save-and-verify. Throws TaskAssignmentEvictedException if the worker no
   * longer owns the task.
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
      // Immediate silence: remove from local registry before throwing
      eventPublisher.publishEvent(new TaskStoppedEvent(task.getId()));
      throw new TaskAssignmentEvictedException(task.getId());
    }
  }

  // --- Protected Hooks ---

  protected void onPreExecute(Task task) {}

  protected void onPostExecuteSuccess(Task task, O output) {}

  protected void onPostExecuteFailure(Task task, Exception e) {}

  protected void onCleanup(Task task) {}
}
