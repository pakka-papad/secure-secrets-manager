package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.security.SecurityUtils;
import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tasks.models.TaskInput;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import com.example.secrets_manager.tracing.CorrelationContext;
import com.example.secrets_manager.tracing.MissingCorrelationContextException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing the lifecycle of background tasks, specifically their creation and
 * persistence.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TaskService {

  private final TaskRepository taskRepository;
  private final TaskEntityConverter taskConverter;

  /**
   * Creates and persists a new background task.
   *
   * <p>Automatically captures the current user from the Security Context and the Correlation ID
   * from the Tracing Context.
   *
   * @param type The type of task to create.
   * @param input The input payload for the task.
   * @return The created Task model.
   * @throws MissingCorrelationContextException if no Correlation ID is present on the thread.
   */
  @Transactional
  public Task createTask(TaskType type, TaskInput input) {
    // Resolve Traceability (Strict Enforcement)
    final var correlationId =
        CorrelationContext.get()
            .orElseThrow(
                () ->
                    new MissingCorrelationContextException(
                        "Task cannot be created without a Correlation ID context."));

    // Resolve Initiator
    final var userId = SecurityUtils.getAuthenticatedUserId();

    // Build Model
    final var task =
        Task.builder()
            .type(type)
            .input(input)
            .state(TaskState.PENDING)
            .initiatorUserId(userId)
            .correlationId(correlationId)
            .build();

    // Persist
    final var entity = taskConverter.fromModel(task);
    final var saved = taskRepository.save(entity);

    log.info(
        "Created background task {} of type {} (Trace: {})", saved.getId(), type, correlationId);

    return taskConverter.toModel(saved);
  }
}
