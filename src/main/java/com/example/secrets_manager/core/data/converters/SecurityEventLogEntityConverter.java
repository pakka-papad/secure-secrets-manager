package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.SecurityEventLogEntity;
import com.example.secrets_manager.core.models.SecurityEvent;
import com.example.secrets_manager.core.models.SecurityEventLog;

public class SecurityEventLogEntityConverter {

  public static SecurityEventLog toModel(SecurityEventLogEntity entity) {
    if (entity == null) {
      return null;
    }

    return SecurityEventLog.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .correlationId(entity.getCorrelationId())
        .actorUserId(entity.getActorUserId())
        .action(SecurityEvent.valueOf(entity.getAction()))
        .targetUserId(entity.getTargetUserId())
        .targetGroupId(entity.getTargetGroupId())
        .targetSecretId(entity.getTargetSecretId())
        .targetMasterKeyVersion(entity.getTargetMasterKeyVersion())
        .details(entity.getDetails())
        .build();
  }

  public static SecurityEventLogEntity fromModel(SecurityEventLog model) {
    if (model == null) {
      return null;
    }

    return SecurityEventLogEntity.builder()
        .id(model.getId())
        .createdAt(model.getCreatedAt())
        .correlationId(model.getCorrelationId())
        .actorUserId(model.getActorUserId())
        .action(model.getAction().name())
        .targetUserId(model.getTargetUserId())
        .targetGroupId(model.getTargetGroupId())
        .targetSecretId(model.getTargetSecretId())
        .targetMasterKeyVersion(model.getTargetMasterKeyVersion())
        .details(model.getDetails())
        .build();
  }
}
