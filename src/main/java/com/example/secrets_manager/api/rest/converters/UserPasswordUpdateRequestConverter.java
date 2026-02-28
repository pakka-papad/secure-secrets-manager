package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.UserPasswordUpdateRequest;
import com.example.secrets_manager.core.models.UserPasswordUpdatePayload;
import java.nio.charset.StandardCharsets;

public final class UserPasswordUpdateRequestConverter {

  private UserPasswordUpdateRequestConverter() {}

  public static UserPasswordUpdatePayload toModel(UserPasswordUpdateRequest request) {
    if (request == null) return null;
    return UserPasswordUpdatePayload.builder()
        .oldPassword(
            request.getOldPassword() != null
                ? request.getOldPassword().getBytes(StandardCharsets.UTF_8)
                : null)
        .newPassword(
            request.getNewPassword() != null
                ? request.getNewPassword().getBytes(StandardCharsets.UTF_8)
                : null)
        .build();
  }
}
