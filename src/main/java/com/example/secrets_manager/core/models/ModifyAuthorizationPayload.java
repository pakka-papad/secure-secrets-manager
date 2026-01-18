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
public class ModifyAuthorizationPayload {
  private UUID actorUserId;
  private UUID targetUserId;
  private UUID groupId;
  private PermissionType permission;
  private boolean grant; // true to grant, false to revoke
}
