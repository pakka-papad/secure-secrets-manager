package com.example.secrets_manager.tasks.models;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Domain model for a system-wide background task. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
  private UUID id;
  private UUID parentTaskId;
  private UUID initiatorUserId;
  private Long initiatorAuditSeqId;
  private Instant createdAt;
  private Instant startedAt;
  private Instant completedAt;

  private TaskType type;
  private TaskState state;

  private TaskInput input;
  private TaskOutput output;
  private TaskStateExtraInfo stateExtraInfo;
  private String metadata; // Generic JSON metadata
}
