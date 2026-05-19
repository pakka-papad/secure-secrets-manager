package com.example.secrets_manager.core.models.search;

import com.example.secrets_manager.core.models.AuditAction;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Criteria for filtering audit logs during searches. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogSearchCriteria {
  private UUID actorUserId;
  private AuditAction action;
  private UUID targetSecretId;
  private UUID correlationId;
  private Instant startTime;
  private Instant endTime;
}
