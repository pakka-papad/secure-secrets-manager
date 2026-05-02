package com.example.secrets_manager.tasks.services;

import com.example.secrets_manager.tasks.models.TaskInput;
import com.example.secrets_manager.tasks.models.TaskOutput;
import com.example.secrets_manager.tasks.models.TaskType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Registry that holds all available TaskHandlers in the system. */
@Service
public class TaskHandlerRegistry {

  private final Map<TaskType, TaskHandler<?, ?>> handlers;

  @Autowired
  public TaskHandlerRegistry(List<TaskHandler<?, ?>> handlerList) {
    this.handlers =
        handlerList.stream()
            .collect(Collectors.toMap(TaskHandler::getSupportedType, Function.identity()));
  }

  /** Retrieves the handler for a specific task type. */
  @SuppressWarnings("unchecked")
  public <I extends TaskInput, O extends TaskOutput> Optional<TaskHandler<I, O>> getHandler(
      TaskType type) {
    return Optional.ofNullable((TaskHandler<I, O>) handlers.get(type));
  }
}
