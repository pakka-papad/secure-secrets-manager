package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.MasterKeyState;

public class MasterKeyEntityConverter {

  public static MasterKey toModel(MasterKeyEntity entity) {
    if (entity == null) {
      return null;
    }

    return MasterKey.builder()
        .version(entity.getVersion())
        .createdAt(entity.getCreatedAt())
        .status(MasterKeyState.valueOf(entity.getStatus()))
        .encryptAlgo(entity.getEncryptAlgo())
        .build();
  }

  public static MasterKeyEntity fromModel(MasterKey model) {
    if (model == null) {
      return null;
    }

    return MasterKeyEntity.builder()
        .version(model.getVersion())
        .createdAt(model.getCreatedAt())
        .status(model.getStatus().name())
        .encryptAlgo(model.getEncryptAlgo())
        .build();
  }
}
