package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.SecretMetadataResponse;
import com.example.secrets_manager.core.models.Secret;

public class SecretMetadataResponseConverter {

  public static SecretMetadataResponse fromModel(Secret secret) {
    if (secret == null) {
      return null;
    }
    return SecretMetadataResponse.builder()
        .id(secret.getId())
        .name(secret.getSecretName())
        .createdAt(secret.getCreatedAt())
        .modifiedAt(secret.getModifiedAt())
        .build();
  }
}
