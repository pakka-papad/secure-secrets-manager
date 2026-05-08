package com.example.secrets_manager.core.models;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEventLogPayload {
  private UUID actorUserId;
  private SecurityEvent action;
  private UUID correlationId;
  private UUID targetUserId;
  private UUID targetGroupId;
  private UUID targetSecretId;
  private Integer targetMasterKeyVersion;
  private String details;
}
