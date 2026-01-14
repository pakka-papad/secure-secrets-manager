package com.example.secrets_manager.core.data.converters;

import com.example.secrets_manager.core.data.entities.AuditLogEntity;
import com.example.secrets_manager.core.models.AuditLog;

public class AuditLogEntityConverter {

  public static AuditLog toModel(AuditLogEntity entity) {
    if (entity == null) {
      return null;
    }

    return AuditLog.builder()
        .seqId(entity.getSeqId())
        .causeSeqId(entity.getCauseSeqId())
        .userId(entity.getUserId())
        .action(entity.getAction())
        .secretId(entity.getSecretId())
        .createdAt(entity.getCreatedAt())
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
        .causeSeqId(model.getCauseSeqId())
        .userId(model.getUserId())
        .action(model.getAction())
        .secretId(model.getSecretId())
        .createdAt(model.getCreatedAt())
        .prevHash(model.getPrevHash())
        .dataHash(model.getDataHash())
        .build();
  }
}
