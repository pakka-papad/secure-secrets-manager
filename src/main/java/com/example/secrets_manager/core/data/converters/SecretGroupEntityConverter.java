package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import com.example.secrets_manager.core.models.SecretGroup;

public class SecretGroupEntityConverter {

    public static SecretGroup toModel(SecretGroupEntity entity) {
        if (entity == null) {
            return null;
        }

        return SecretGroup.builder()
                .id(entity.getId())
                .name(entity.getName())
                .dataKeyLength(entity.getDataKeyLength())
                .encryptAlgo(entity.getEncryptAlgo())
                .createdAt(entity.getCreatedAt())
                .modifiedAt(entity.getModifiedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public static SecretGroupEntity fromModel(SecretGroup model) {
        if (model == null) {
            return null;
        }

        return SecretGroupEntity.builder()
                .id(model.getId())
                .name(model.getName())
                .dataKeyLength(model.getDataKeyLength())
                .encryptAlgo(model.getEncryptAlgo())
                .createdAt(model.getCreatedAt())
                .modifiedAt(model.getModifiedAt())
                .deletedAt(model.getDeletedAt())
                .build();
    }
}