package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationDetailedResponse;
import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationResponse;
import com.example.secrets_manager.core.models.SecretGroupAuthorization;
import com.example.secrets_manager.core.models.SecretGroupAuthorizationDetailed;

public final class SecretGroupAuthorizationResponseConverter {

  private SecretGroupAuthorizationResponseConverter() {}

  public static SecretGroupAuthorizationResponse fromModel(SecretGroupAuthorization model) {
    if (model == null) {
      return null;
    }

    return SecretGroupAuthorizationResponse.builder()
        .userId(model.getUserId())
        .groupId(model.getGroupId())
        .permissions(model.getPermissions())
        .modifiedAt(model.getModifiedAt())
        .build();
  }

  public static SecretGroupAuthorizationDetailedResponse fromDetailedModel(
      SecretGroupAuthorizationDetailed model) {
    if (model == null) {
      return null;
    }

    return SecretGroupAuthorizationDetailedResponse.builder()
        .userId(model.getUserId())
        .username(model.getUsername())
        .groupId(model.getGroupId())
        .permissions(model.getPermissions())
        .modifiedAt(model.getModifiedAt())
        .build();
  }
}
