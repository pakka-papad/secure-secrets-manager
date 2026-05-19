package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.AuditLogDetailedResponse;
import com.example.secrets_manager.api.rest.dto.AuditLogSummaryResponse;
import com.example.secrets_manager.core.data.repositories.AuditLogInfo;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLog;
import java.util.Base64;

/** Converter for transforming between AuditLog (Domain/Projection) and API DTOs. */
public final class AuditLogResponseConverter {

  private AuditLogResponseConverter() {
    // Prevent instantiation
  }

  public static AuditLogSummaryResponse toSummaryResponse(AuditLogInfo info) {
    if (info == null) {
      return null;
    }

    return AuditLogSummaryResponse.builder()
        .seqId(info.getSeqId())
        .correlationId(info.getCorrelationId())
        .createdAt(info.getCreatedAt())
        .actorUserId(info.getActorUserId())
        .action(AuditAction.valueOf(info.getAction()))
        .targetUserId(info.getTargetUserId())
        .targetGroupId(info.getTargetGroupId())
        .targetSecretId(info.getTargetSecretId())
        .targetMasterKeyVersion(info.getTargetMasterKeyVersion())
        .build();
  }

  public static AuditLogDetailedResponse toDetailedResponse(AuditLog model) {
    if (model == null) {
      return null;
    }

    return AuditLogDetailedResponse.builder()
        .seqId(model.getSeqId())
        .correlationId(model.getCorrelationId())
        .createdAt(model.getCreatedAt())
        .actorUserId(model.getActorUserId())
        .action(model.getAction())
        .targetUserId(model.getTargetUserId())
        .targetGroupId(model.getTargetGroupId())
        .targetSecretId(model.getTargetSecretId())
        .targetMasterKeyVersion(model.getTargetMasterKeyVersion())
        .details(model.getDetails())
        .prevHash(Base64.getEncoder().encodeToString(model.getPrevHash()))
        .dataHash(Base64.getEncoder().encodeToString(model.getDataHash()))
        .build();
  }
}
