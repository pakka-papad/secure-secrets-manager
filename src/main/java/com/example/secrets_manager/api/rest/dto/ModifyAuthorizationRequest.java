package com.example.secrets_manager.api.rest.dto;

import com.example.secrets_manager.core.models.PermissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyAuthorizationRequest {

  @Schema(
      description = "The set of permissions to assign to the user.",
      example = "[\"READ\", \"WRITE\"]")
  @NotNull
  private Set<PermissionType> permissions;
}
