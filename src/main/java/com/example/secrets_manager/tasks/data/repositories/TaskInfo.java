package com.example.secrets_manager.tasks.data.repositories;

import java.time.Instant;
import java.util.UUID;

/** Surgical projection for background task summary information. */
public interface TaskInfo {
  UUID getId();

  String getType();

  String getState();

  Instant getCreatedAt();

  Instant getStartedAt();

  Instant getCompletedAt();

  UUID getInitiatorUserId();

  UUID getCorrelationId();
}
