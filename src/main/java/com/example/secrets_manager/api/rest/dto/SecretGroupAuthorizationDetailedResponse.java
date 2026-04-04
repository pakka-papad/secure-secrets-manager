package com.example.secrets_manager.api.rest.dto;

import com.example.secrets_manager.core.models.PermissionType;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretGroupAuthorizationDetailedResponse {
  private UUID userId;
  private String username;
  private UUID groupId;
  private EnumSet<PermissionType> permissions;
  private Instant modifiedAt;
}
