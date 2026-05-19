package com.example.secrets_manager.core.data.repositories;

import java.time.Instant;
import java.util.UUID;

/** Surgical projection for audit log summary information. */
public interface AuditLogInfo {
  Long getSeqId();

  UUID getCorrelationId();

  Instant getCreatedAt();

  UUID getActorUserId();

  String getAction();

  UUID getTargetUserId();

  UUID getTargetGroupId();

  UUID getTargetSecretId();

  Integer getTargetMasterKeyVersion();
}
