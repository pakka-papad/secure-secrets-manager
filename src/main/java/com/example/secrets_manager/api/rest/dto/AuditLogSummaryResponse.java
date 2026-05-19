package com.example.secrets_manager.api.rest.dto;

import com.example.secrets_manager.core.models.AuditAction;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Basic information about an audit log entry for list views. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogSummaryResponse {
  private Long seqId;
  private UUID correlationId;
  private Instant createdAt;
  private UUID actorUserId;
  private AuditAction action;
  private UUID targetUserId;
  private UUID targetGroupId;
  private UUID targetSecretId;
  private Integer targetMasterKeyVersion;
}
