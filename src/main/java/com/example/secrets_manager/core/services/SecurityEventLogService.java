package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.SecurityEventLogEntityConverter;
import com.example.secrets_manager.core.data.repositories.SecurityEventLogRepository;
import com.example.secrets_manager.core.models.SecurityEventLog;
import com.example.secrets_manager.core.models.SecurityEventLogPayload;
import com.example.secrets_manager.tracing.CorrelationContext;
import com.example.secrets_manager.tracing.MissingCorrelationContextException;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityEventLogService {

  private final SecurityEventLogRepository securityEventLogRepository;

  @Autowired
  public SecurityEventLogService(SecurityEventLogRepository securityEventLogRepository) {
    this.securityEventLogRepository = securityEventLogRepository;
  }

  /**
   * Persists a security event in a brand-new transaction. This ensures the event is recorded even
   * if the calling transaction rolls back.
   *
   * @param payload The SecurityEventLogPayload containing the event details.
   * @return The persisted SecurityEventLog model.
   * @throws MissingCorrelationContextException if no correlation ID is found in payload or context.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SecurityEventLog save(SecurityEventLogPayload payload) {
    // Resolve Correlation ID (Strict Enforcement)
    final var correlationId =
        payload.getCorrelationId() != null
            ? payload.getCorrelationId()
            : CorrelationContext.get()
                .orElseThrow(
                    () ->
                        new MissingCorrelationContextException(
                            "Security event cannot be saved without a Correlation ID. Traceability is mandatory."));

    var event =
        SecurityEventLog.builder()
            .actorUserId(payload.getActorUserId())
            .action(payload.getAction())
            .correlationId(correlationId)
            .targetUserId(payload.getTargetUserId())
            .targetGroupId(payload.getTargetGroupId())
            .targetSecretId(payload.getTargetSecretId())
            .targetMasterKeyVersion(payload.getTargetMasterKeyVersion())
            .details(payload.getDetails())
            .createdAt(Instant.now())
            .build();

    var entityToSave = SecurityEventLogEntityConverter.fromModel(event);
    var savedEntity = securityEventLogRepository.save(entityToSave);

    return SecurityEventLogEntityConverter.toModel(savedEntity);
  }
}
