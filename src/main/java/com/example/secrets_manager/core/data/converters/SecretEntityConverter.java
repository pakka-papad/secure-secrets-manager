package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.SecretEntity;
import com.example.secrets_manager.core.models.Secret;

public class SecretEntityConverter {

    public static Secret toModel(SecretEntity entity) {
        if (entity == null) {
            return null;
        }

        return Secret.builder()
                .id(entity.getId())
                .groupId(entity.getGroupId())
                .secretName(entity.getSecretName())
                .encryptedValue(entity.getEncryptedValue())
                .dataEncryptionKey(entity.getDataEncryptionKey())
                .dataKeyVersion(entity.getDataKeyVersion())
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
                .encryptedValue(model.getEncryptedValue())
                .dataEncryptionKey(model.getDataEncryptionKey())
                .dataKeyVersion(model.getDataKeyVersion())
                .masterKeyVersion(model.getMasterKeyVersion())
                .createdAt(model.getCreatedAt())
                .modifiedAt(model.getModifiedAt())
                .deletedAt(model.getDeletedAt())
                .build();
    }
}