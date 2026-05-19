package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.AuditLogEntity;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLog;

public class AuditLogEntityConverter {

  public static AuditLog toModel(AuditLogEntity entity) {
    if (entity == null) {
      return null;
    }

    return AuditLog.builder()
        .seqId(entity.getSeqId())
        .correlationId(entity.getCorrelationId())
        .createdAt(entity.getCreatedAt())
        .actorUserId(entity.getActorUserId())
        .action(AuditAction.valueOf(entity.getAction()))
        .targetUserId(entity.getTargetUserId())
        .targetGroupId(entity.getTargetGroupId())
        .targetSecretId(entity.getTargetSecretId())
        .targetMasterKeyVersion(entity.getTargetMasterKeyVersion())
        .details(entity.getDetails())
        .prevHash(entity.getPrevHash())
        .dataHash(entity.getDataHash())
        .build();
  }

  public static AuditLogEntity fromModel(AuditLog model) {
    if (model == null) {
      return null;
    }

    return AuditLogEntity.builder()
        .seqId(model.getSeqId())
        .correlationId(model.getCorrelationId())
        .createdAt(model.getCreatedAt())
        .actorUserId(model.getActorUserId())
        .action(model.getAction().name())
        .targetUserId(model.getTargetUserId())
        .targetGroupId(model.getTargetGroupId())
        .targetSecretId(model.getTargetSecretId())
        .targetMasterKeyVersion(model.getTargetMasterKeyVersion())
        .details(model.getDetails())
        .prevHash(model.getPrevHash())
        .dataHash(model.getDataHash())
        .build();
  }
}
