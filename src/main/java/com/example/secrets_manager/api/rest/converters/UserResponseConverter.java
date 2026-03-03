package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.UserResponse;
import com.example.secrets_manager.core.models.User;

/** Converter to transform User domain model into UserResponse DTO. */
public final class UserResponseConverter {

  private UserResponseConverter() {}

  public static UserResponse fromModel(User model) {
    if (model == null) {
      return null;
    }

    return UserResponse.builder()
        .id(model.getId())
        .name(model.getName())
        .roles(model.getRoles())
        .createdAt(model.getCreatedAt())
        .modifiedAt(model.getModifiedAt())
        .build();
  }
}
