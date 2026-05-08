package com.example.secrets_manager.core.models;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
  private Long seqId;
  private Long causeSeqId;
  private UUID correlationId;
  private Instant createdAt;
  private UUID actorUserId;
  private AuditAction action;
  private UUID targetUserId;
  private UUID targetGroupId;
  private UUID targetSecretId;
  private Integer targetMasterKeyVersion;
  private String details;
  @ToString.Exclude private byte[] prevHash;
  @ToString.Exclude private byte[] dataHash;
}
