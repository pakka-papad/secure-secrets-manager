package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationId;
import com.example.secrets_manager.core.models.SecretGroupAuthorization;

public class SecretGroupAuthorizationEntityConverter {

  public static SecretGroupAuthorization toModel(SecretGroupAuthorizationEntity entity) {
    if (entity == null) {
      return null;
    }

    return SecretGroupAuthorization.builder()
        .userId(entity.getId().getUserId())
        .groupId(entity.getId().getGroupId())
        .pRead(entity.isPRead())
        .pWrite(entity.isPWrite())
        .pDelete(entity.isPDelete())
        .modifiedAt(entity.getModifiedAt())
        .build();
  }

  public static SecretGroupAuthorizationEntity fromModel(SecretGroupAuthorization model) {
    if (model == null) {
      return null;
    }

    SecretGroupAuthorizationId id =
        new SecretGroupAuthorizationId(model.getUserId(), model.getGroupId());

    return SecretGroupAuthorizationEntity.builder()
        .id(id)
        .pRead(model.isPRead())
        .pWrite(model.isPWrite())
        .pDelete(model.isPDelete())
        .modifiedAt(model.getModifiedAt())
        .build();
  }
}
