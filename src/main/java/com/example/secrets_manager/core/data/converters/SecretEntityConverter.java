package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.SecretEntity;
import com.example.secrets_manager.core.models.Secret;
import com.example.secrets_manager.crypto.dto.EncryptedData;

public class SecretEntityConverter {

  public static Secret toModel(SecretEntity entity) {
    if (entity == null) {
      return null;
    }

    String secretAlgo = entity.getGroup() != null ? entity.getGroup().getEncryptAlgo() : null;
    String dekAlgo = entity.getMasterKey() != null ? entity.getMasterKey().getEncryptAlgo() : null;

    return Secret.builder()
        .id(entity.getId())
        .groupId(entity.getGroupId())
        .secretName(entity.getSecretName())
        .valueEnvelope(
            new EncryptedData(
                entity.getValueCiphertext(),
                entity.getValueNonce(),
                entity.getValueAuthTag(),
                secretAlgo))
        .dekEnvelope(
            new EncryptedData(
                entity.getDekCiphertext(), entity.getDekNonce(), entity.getDekAuthTag(), dekAlgo))
        .dekVersion(entity.getDekVersion())
        .masterKeyVersion(entity.getMasterKeyVersion())
        .createdAt(entity.getCreatedAt())
        .modifiedAt(entity.getModifiedAt())
        .deletedAt(entity.getDeletedAt())
        .build();
  }

  public static SecretEntity fromModel(Secret model) {
    if (model == null) {
      return null;
    }

    return SecretEntity.builder()
        .id(model.getId())
        .groupId(model.getGroupId())
        .secretName(model.getSecretName())
        .valueCiphertext(model.getValueEnvelope().getCiphertext())
        .valueNonce(model.getValueEnvelope().getNonce())
        .valueAuthTag(model.getValueEnvelope().getAuthTag())
        .dekCiphertext(model.getDekEnvelope().getCiphertext())
        .dekNonce(model.getDekEnvelope().getNonce())
        .dekAuthTag(model.getDekEnvelope().getAuthTag())
        .dekVersion(model.getDekVersion())
        .masterKeyVersion(model.getMasterKeyVersion())
        .createdAt(model.getCreatedAt())
        .modifiedAt(model.getModifiedAt())
        .deletedAt(model.getDeletedAt())
        .build();
  }
}
