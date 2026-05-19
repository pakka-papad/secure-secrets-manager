package com.example.secrets_manager.api.rest.converters;

import com.example.secrets_manager.api.rest.dto.SecurityEventDetailedResponse;
import com.example.secrets_manager.api.rest.dto.SecurityEventSummaryResponse;
import com.example.secrets_manager.core.data.repositories.SecurityEventLogInfo;
import com.example.secrets_manager.core.models.SecurityEvent;
import com.example.secrets_manager.core.models.SecurityEventLog;

/** Converter for transforming between SecurityEventLog (Domain/Projection) and API DTOs. */
public final class SecurityEventLogResponseConverter {

  private SecurityEventLogResponseConverter() {
    // Prevent instantiation
  }

  public static SecurityEventSummaryResponse toSummaryResponse(SecurityEventLogInfo info) {
    if (info == null) {
      return null;
    }

    return SecurityEventSummaryResponse.builder()
        .id(info.getId())
        .correlationId(info.getCorrelationId())
        .createdAt(info.getCreatedAt())
        .actorUserId(info.getActorUserId())
        .action(SecurityEvent.valueOf(info.getAction()))
        .targetUserId(info.getTargetUserId())
        .targetGroupId(info.getTargetGroupId())
        .targetSecretId(info.getTargetSecretId())
        .targetMasterKeyVersion(info.getTargetMasterKeyVersion())
        .build();
  }

  public static SecurityEventDetailedResponse toDetailedResponse(SecurityEventLog model) {
    if (model == null) {
      return null;
    }

    return SecurityEventDetailedResponse.builder()
        .id(model.getId())
        .correlationId(model.getCorrelationId())
        .createdAt(model.getCreatedAt())
        .actorUserId(model.getActorUserId())
        .action(model.getAction())
        .targetUserId(model.getTargetUserId())
        .targetGroupId(model.getTargetGroupId())
        .targetSecretId(model.getTargetSecretId())
        .targetMasterKeyVersion(model.getTargetMasterKeyVersion())
        .details(model.getDetails())
        .build();
  }
}
