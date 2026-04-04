package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationResponse;
import com.example.secrets_manager.core.models.SecretGroupAuthorization;

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
}
