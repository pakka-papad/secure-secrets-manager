package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.AuthorizationEntity;
import com.example.secrets_manager.core.data.entities.AuthorizationId;
import com.example.secrets_manager.core.models.Authorization;

public class AuthorizationEntityConverter {

  public static Authorization toModel(AuthorizationEntity entity) {
    if (entity == null) {
      return null;
    }

    return Authorization.builder()
        .userId(entity.getId().getUserId())
        .groupId(entity.getId().getGroupId())
        .pRead(entity.isPRead())
        .pWrite(entity.isPWrite())
        .modifiedAt(entity.getModifiedAt())
        .build();
  }

  public static AuthorizationEntity fromModel(Authorization model) {
    if (model == null) {
      return null;
    }

    AuthorizationId id =
        AuthorizationId.builder().userId(model.getUserId()).groupId(model.getGroupId()).build();

    return AuthorizationEntity.builder()
        .id(id)
        .pRead(model.isPRead())
        .pWrite(model.isPWrite())
        .modifiedAt(model.getModifiedAt())
        .build();
  }
}
