package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.models.TaskType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Registry that discovers and maintains all available TaskHandler implementations. Provides lookup
 * capabilities by TaskType for the execution engine.
 */
@Service
public class TaskHandlerRegistry {

  private final Map<TaskType, TaskHandler> handlers;

  @Autowired
  public TaskHandlerRegistry(List<TaskHandler> handlerList) {
    this.handlers =
        handlerList.stream()
            .collect(Collectors.toMap(TaskHandler::getSupportedType, Function.identity()));
  }

  /**
   * Retrieves the handler for a specific task type.
   *
   * @param type The task type.
   * @return An Optional containing the handler, or empty if not found.
   */
  public Optional<TaskHandler> getHandler(TaskType type) {
    return Optional.ofNullable(handlers.get(type));
  }
}
