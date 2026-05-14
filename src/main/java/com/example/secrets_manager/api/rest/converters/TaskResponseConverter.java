package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.TaskDetailedResponse;
import com.example.secrets_manager.api.rest.dto.TaskSummaryResponse;
import com.example.secrets_manager.tasks.data.repositories.TaskInfo;
import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;

/** Converter for transforming between Task (Domain) and Task-related API DTOs. */
public final class TaskResponseConverter {

  private TaskResponseConverter() {
    // Prevent instantiation
  }

  public static TaskSummaryResponse toSummaryResponse(Task model) {
    if (model == null) {
      return null;
    }

    return TaskSummaryResponse.builder()
        .id(model.getId())
        .type(model.getType())
        .state(model.getState())
        .createdAt(model.getCreatedAt())
        .startedAt(model.getStartedAt())
        .completedAt(model.getCompletedAt())
        .initiatorUserId(model.getInitiatorUserId())
        .correlationId(model.getCorrelationId())
        .build();
  }

  public static TaskSummaryResponse toSummaryResponse(TaskInfo info) {
    if (info == null) {
      return null;
    }

    return TaskSummaryResponse.builder()
        .id(info.getId())
        .type(TaskType.valueOf(info.getType()))
        .state(TaskState.valueOf(info.getState()))
        .createdAt(info.getCreatedAt())
        .startedAt(info.getStartedAt())
        .completedAt(info.getCompletedAt())
        .initiatorUserId(info.getInitiatorUserId())
        .correlationId(info.getCorrelationId())
        .build();
  }

  public static TaskDetailedResponse toDetailedResponse(Task model) {
    if (model == null) {
      return null;
    }

    return TaskDetailedResponse.builder()
        .id(model.getId())
        .type(model.getType())
        .state(model.getState())
        .createdAt(model.getCreatedAt())
        .startedAt(model.getStartedAt())
        .completedAt(model.getCompletedAt())
        .initiatorUserId(model.getInitiatorUserId())
        .correlationId(model.getCorrelationId())
        .parentTaskId(model.getParentTaskId())
        .input(model.getInput())
        .output(model.getOutput())
        .stateExtraInfo(model.getStateExtraInfo())
        .metadata(model.getMetadata())
        .build();
  }
}
