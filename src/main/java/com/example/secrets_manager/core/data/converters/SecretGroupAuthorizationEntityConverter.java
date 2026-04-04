package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationId;
import com.example.secrets_manager.core.models.PermissionType;
import com.example.secrets_manager.core.models.SecretGroupAuthorization;
import java.util.EnumSet;
import java.util.Set;

public final class SecretGroupAuthorizationEntityConverter {

  private SecretGroupAuthorizationEntityConverter() {}

  public static SecretGroupAuthorization toModel(SecretGroupAuthorizationEntity entity) {
    if (entity == null) {
      return null;
    }

    Set<PermissionType> permissions = EnumSet.noneOf(PermissionType.class);
    if (entity.isPRead()) permissions.add(PermissionType.READ);
    if (entity.isPWrite()) permissions.add(PermissionType.WRITE);
    if (entity.isPDelete()) permissions.add(PermissionType.DELETE);

    return SecretGroupAuthorization.builder()
        .userId(entity.getId().getUserId())
        .groupId(entity.getId().getGroupId())
        .permissions(EnumSet.copyOf(permissions))
        .modifiedAt(entity.getModifiedAt())
        .build();
  }

  public static SecretGroupAuthorizationEntity fromModel(SecretGroupAuthorization model) {
    if (model == null) {
      return null;
    }

    SecretGroupAuthorizationId id =
        new SecretGroupAuthorizationId(model.getUserId(), model.getGroupId());

    EnumSet<PermissionType> permissions = model.getPermissions();
    return SecretGroupAuthorizationEntity.builder()
        .id(id)
        .pRead(permissions.contains(PermissionType.READ))
        .pWrite(permissions.contains(PermissionType.WRITE))
        .pDelete(permissions.contains(PermissionType.DELETE))
        .modifiedAt(model.getModifiedAt())
        .build();
  }
}
