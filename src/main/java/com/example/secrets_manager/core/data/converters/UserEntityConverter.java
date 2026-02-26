package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.models.UserRole;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

public class UserEntityConverter {

  public static User toModel(UserEntity entity) {
    if (entity == null) {
      return null;
    }

    var roles =
        (entity.getRoles() == null || entity.getRoles().length == 0)
            ? EnumSet.noneOf(UserRole.class)
            : Arrays.stream(entity.getRoles())
                .map(UserRole::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));

    return User.builder()
        .id(entity.getId())
        .name(entity.getName())
        .pwSalt(entity.getPwSalt())
        .pwDigest(entity.getPwDigest())
        .createdAt(entity.getCreatedAt())
        .modifiedAt(entity.getModifiedAt())
        .hashAlgo(entity.getHashAlgo())
        .hashParams(entity.getHashParams())
        .roles(roles)
        .deletedAt(entity.getDeletedAt())
        .build();
  }

  public static UserEntity fromModel(User model) {
    if (model == null) {
      return null;
    }

    var roles =
        (model.getRoles() == null || model.getRoles().isEmpty())
            ? new String[0]
            : model.getRoles().stream().map(Enum::name).toArray(String[]::new);

    return UserEntity.builder()
        .id(model.getId())
        .name(model.getName())
        .pwSalt(model.getPwSalt())
        .pwDigest(model.getPwDigest())
        .createdAt(model.getCreatedAt())
        .modifiedAt(model.getModifiedAt())
        .hashAlgo(model.getHashAlgo())
        .hashParams(model.getHashParams())
        .roles(roles)
        .deletedAt(model.getDeletedAt())
        .build();
  }
}
