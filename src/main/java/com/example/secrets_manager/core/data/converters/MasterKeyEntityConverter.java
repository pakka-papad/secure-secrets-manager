package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import com.example.secrets_manager.core.models.MasterKey;

public class MasterKeyEntityConverter {

  public static MasterKey toModel(MasterKeyEntity entity) {
    if (entity == null) {
      return null;
    }

    return MasterKey.builder()
        .version(entity.getVersion())
        .createdAt(entity.getCreatedAt())
        .status(entity.getStatus())
        .build();
  }

  public static MasterKeyEntity fromModel(MasterKey model) {
    if (model == null) {
      return null;
    }

    return MasterKeyEntity.builder()
        .version(model.getVersion())
        .createdAt(model.getCreatedAt())
        .status(model.getStatus())
        .build();
  }
}
