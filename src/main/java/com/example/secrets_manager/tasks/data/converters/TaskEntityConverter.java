package com.example.secrets_manager.tasks.data.converters;

import com.example.secrets_manager.tasks.data.entities.TaskEntity;
import com.example.secrets_manager.tasks.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Converter for transforming between TaskEntity (Persistence) and Task (Domain). Handles the JSON
 * serialization/deserialization of polymorphic payloads.
 */
@Component
@Slf4j
public class TaskEntityConverter {

  private final ObjectMapper objectMapper;

  public TaskEntityConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Task toModel(TaskEntity entity) {
    if (entity == null) {
      return null;
    }

    return Task.builder()
        .id(entity.getId())
        .correlationId(entity.getCorrelationId())
        .parentTaskId(entity.getParentTaskId())
        .initiatorUserId(entity.getInitiatorUserId())
        .createdAt(entity.getCreatedAt())
        .startedAt(entity.getStartedAt())
        .completedAt(entity.getCompletedAt())
        .type(TaskType.valueOf(entity.getType()))
        .state(TaskState.valueOf(entity.getState()))
        .input(deserialize(entity.getTaskInput(), TaskInput.class))
        .output(deserialize(entity.getTaskOutput(), TaskOutput.class))
        .stateExtraInfo(deserialize(entity.getStateExtraInfo(), TaskStateExtraInfo.class))
        .metadata(entity.getMetadata())
        .build();
  }

  public TaskEntity fromModel(Task model) {
    if (model == null) {
      return null;
    }

    return TaskEntity.builder()
        .id(model.getId())
        .correlationId(model.getCorrelationId())
        .parentTaskId(model.getParentTaskId())
        .initiatorUserId(model.getInitiatorUserId())
        .createdAt(model.getCreatedAt())
        .startedAt(model.getStartedAt())
        .completedAt(model.getCompletedAt())
        .type(model.getType().name())
        .state(model.getState().name())
        .taskInput(serialize(model.getInput()))
        .taskOutput(serialize(model.getOutput()))
        .stateExtraInfo(serialize(model.getStateExtraInfo()))
        .metadata(model.getMetadata())
        .build();
  }

  private <T> T deserialize(String json, Class<T> type) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException e) {
      log.error(
          "Failed to deserialize task payload to {}: {}", type.getSimpleName(), e.getMessage());
      return null;
    }
  }

  private String serialize(Object payload) {
    if (payload == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize task payload: {}", e.getMessage());
      return null;
    }
  }
}
