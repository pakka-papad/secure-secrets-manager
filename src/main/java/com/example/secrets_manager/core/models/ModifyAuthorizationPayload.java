package com.example.secrets_manager.core.models;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload representing the desired set of permissions for a user on a secret group. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyAuthorizationPayload {
  @NotNull private UUID targetUserId;
  @NotNull private UUID groupId;
  @NotEmpty private Set<PermissionType> permissions;
}
