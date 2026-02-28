package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.LoginRequest;
import com.example.secrets_manager.core.models.LoginPayload;
import java.nio.charset.StandardCharsets;

public final class LoginRequestConverter {

  private LoginRequestConverter() {}

  public static LoginPayload toModel(LoginRequest request) {
    if (request == null) return null;
    return LoginPayload.builder()
        .username(request.getUsername())
        .password(
            request.getPassword() != null
                ? request.getPassword().getBytes(StandardCharsets.UTF_8)
                : null)
        .build();
  }
}
