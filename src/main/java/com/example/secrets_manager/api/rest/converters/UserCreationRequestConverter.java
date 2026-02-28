package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.UserCreationRequest;
import com.example.secrets_manager.core.models.UserCreationPayload;
import java.nio.charset.StandardCharsets;

public final class UserCreationRequestConverter {

  private UserCreationRequestConverter() {}

  public static UserCreationPayload toModel(UserCreationRequest request) {
    if (request == null) return null;
    return UserCreationPayload.builder()
        .name(request.getName())
        .password(
            request.getPassword() != null
                ? request.getPassword().getBytes(StandardCharsets.UTF_8)
                : null)
        .roles(request.getRoles())
        .build();
  }
}
