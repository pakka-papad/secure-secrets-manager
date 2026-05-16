package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.*;
import com.example.secrets_manager.tasks.models.events.TaskStartedEvent;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import java.time.Instant;
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
   * Handles eviction cases gracefully.
   */
  @Transactional
  public void failTask(Task task, Exception e, Consumer<Exception> failureHook) {
    if (e instanceof TaskAssignmentEvictedException) {
      log.error("Task {} failed due to hard eviction. Skipping state persistence.", task.getId());
      return;
    }

    task.setState(TaskState.FAILED);
    task.setCompletedAt(Instant.now());

    try {
      persistStateWithFencing(task);
      failureHook.accept(e);
    } catch (TaskAssignmentEvictedException evictedEx) {
      log.warn(
          "Evicted while attempting to persist FAILED state for task {}. Aborting failure hooks.",
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
   * Performs an atomic save-and-verify against the database. Throws TaskAssignmentEvictedException
   * if the worker no longer owns the task.
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
            entity.getStateExtraInfo());

    if (updated == 0) {
      throw new TaskAssignmentEvictedException(task.getId());
    }
  }
}
