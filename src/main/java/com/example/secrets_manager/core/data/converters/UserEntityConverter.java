package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.models.User;

public class UserEntityConverter {

    public static User toModel(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .pwSalt(entity.getPwSalt())
                .pwDigest(entity.getPwDigest())
                .createdAt(entity.getCreatedAt())
                .modifiedAt(entity.getModifiedAt())
                .hashAlgo(entity.getHashAlgo())
                .hashParams(entity.getHashParams())
                .build();
    }

    public static UserEntity fromModel(User model) {
        if (model == null) {
            return null;
        }

        return UserEntity.builder()
                .id(model.getId())
                .name(model.getName())
                .pwSalt(model.getPwSalt())
                .pwDigest(model.getPwDigest())
                .createdAt(model.getCreatedAt())
                .modifiedAt(model.getModifiedAt())
                .hashAlgo(model.getHashAlgo())
                .hashParams(model.getHashParams())
                .build();
    }
}