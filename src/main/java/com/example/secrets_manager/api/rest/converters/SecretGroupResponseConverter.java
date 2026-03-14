package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.SecretGroupResponse;
import com.example.secrets_manager.core.models.SecretGroup;

public class SecretGroupResponseConverter {

  public static SecretGroupResponse fromModel(SecretGroup model) {
    if (model == null) {
      return null;
    }

    return SecretGroupResponse.builder()
        .id(model.getId())
        .name(model.getName())
        .encryptAlgo(model.getEncryptAlgo())
        .createdAt(model.getCreatedAt())
        .modifiedAt(model.getModifiedAt())
        .build();
  }
}
