package com.example.secrets_manager.api.rest.dto;

import com.example.secrets_manager.tasks.models.TaskInput;
import com.example.secrets_manager.tasks.models.TaskOutput;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskStateExtraInfo;
import com.example.secrets_manager.tasks.models.TaskType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Detailed information about a background task, including input/output payloads. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDetailedResponse {
  private UUID id;
  private TaskType type;
  private TaskState state;
  private Instant createdAt;
  private Instant startedAt;
  private Instant completedAt;
  private UUID initiatorUserId;
  private UUID correlationId;
  private UUID parentTaskId;
  private TaskInput input;
  private TaskOutput output;
  private TaskStateExtraInfo stateExtraInfo;
  private String metadata;
}
