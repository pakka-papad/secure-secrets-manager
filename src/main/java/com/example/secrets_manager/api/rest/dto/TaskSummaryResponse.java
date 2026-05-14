package com.example.secrets_manager.api.rest.dto;

import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Basic information about a background task for list views. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSummaryResponse {
  private UUID id;
  private TaskType type;
  private TaskState state;
  private Instant createdAt;
  private Instant startedAt;
  private Instant completedAt;
  private UUID initiatorUserId;
  private UUID correlationId;
}
