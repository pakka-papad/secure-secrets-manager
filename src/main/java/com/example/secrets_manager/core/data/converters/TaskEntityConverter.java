package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.TaskEntity;
import com.example.secrets_manager.core.models.Task;

public class TaskEntityConverter {

  public static Task toModel(TaskEntity entity) {
    if (entity == null) {
      return null;
    }

    return Task.builder()
        .id(entity.getId())
        .parentTaskId(entity.getParentTaskId())
        .initiatorUserId(entity.getInitiatorUserId())
        .initiatorAuditSeqId(entity.getInitiatorAuditSeqId())
        .createdAt(entity.getCreatedAt())
        .startedAt(entity.getStartedAt())
        .completedAt(entity.getCompletedAt())
        .type(entity.getType())
        .taskInput(entity.getTaskInput())
        .state(entity.getState())
        .stateExtraInfo(entity.getStateExtraInfo())
        .taskOutput(entity.getTaskOutput())
        .metadata(entity.getMetadata())
        .build();
  }

  public static TaskEntity fromModel(Task model) {
    if (model == null) {
      return null;
    }

    return TaskEntity.builder()
        .id(model.getId())
        .parentTaskId(model.getParentTaskId())
        .initiatorUserId(model.getInitiatorUserId())
        .initiatorAuditSeqId(model.getInitiatorAuditSeqId())
        .createdAt(model.getCreatedAt())
        .startedAt(model.getStartedAt())
        .completedAt(model.getCompletedAt())
        .type(model.getType())
        .taskInput(model.getTaskInput())
        .state(model.getState())
        .stateExtraInfo(model.getStateExtraInfo())
        .taskOutput(model.getTaskOutput())
        .metadata(model.getMetadata())
        .build();
  }
}
