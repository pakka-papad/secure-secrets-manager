package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.security.SecurityUtils;
import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.entities.TaskEntity;
import com.example.secrets_manager.tasks.data.repositories.TaskInfo;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.data.repositories.TaskSpecifications;
import com.example.secrets_manager.tasks.models.*;
import com.example.secrets_manager.tracing.CorrelationContext;
import com.example.secrets_manager.tracing.MissingCorrelationContextException;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing the lifecycle of background tasks, specifically their creation, persistence,
 * and retrieval.
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

  /**
   * Retrieves a paginated list of background tasks based on the provided search criteria. This
   * operation is restricted to administrators and always sorts results in reverse chronological
   * order by ID (UUIDv7).
   *
   * @param criteria The filtering criteria.
   * @param pageable The pagination parameters (sort order is ignored).
   * @return A paginated list of task summary info.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public Page<TaskInfo> listTasks(TaskSearchCriteria criteria, Pageable pageable) {
    // Force reverse chronological order using UUIDv7 property
    Pageable sortedPageable =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, TaskEntity.COL_ID));

    Specification<TaskEntity> spec = TaskSpecifications.withCriteria(criteria);

    // Use dynamic projection to avoid fetching large JSONB columns in list views
    return taskRepository.findBy(spec, q -> q.as(TaskInfo.class).page(sortedPageable));
  }

  /**
   * Retrieves the full details of a specific task by its ID. This operation is restricted to
   * administrators.
   *
   * @param taskId The ID of the task to retrieve.
   * @return The task domain model.
   * @throws EntityNotFoundException if the task is not found.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public Task getTaskById(UUID taskId) {
    return taskRepository
        .findById(taskId)
        .map(taskConverter::toModel)
        .orElseThrow(() -> new EntityNotFoundException("Task not found with ID: " + taskId));
  }
}
