package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.SecretGroupCreationRequest;
import com.example.secrets_manager.core.models.SecretGroupCreationPayload;

public class SecretGroupCreationRequestConverter {

  public static SecretGroupCreationPayload toModel(SecretGroupCreationRequest request) {
    if (request == null) {
      return null;
    }

    return SecretGroupCreationPayload.builder()
        .name(request.getName())
        .encryptAlgo(request.getEncryptAlgo())
        .build();
  }
}
