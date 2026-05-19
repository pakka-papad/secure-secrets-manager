package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.SecurityEventLogEntityConverter;
import com.example.secrets_manager.core.data.entities.SecurityEventLogEntity;
import com.example.secrets_manager.core.data.repositories.SecurityEventLogInfo;
import com.example.secrets_manager.core.data.repositories.SecurityEventLogRepository;
import com.example.secrets_manager.core.data.repositories.SecurityEventLogSpecifications;
import com.example.secrets_manager.core.models.SecurityEventLog;
import com.example.secrets_manager.core.models.SecurityEventLogPayload;
import com.example.secrets_manager.core.models.search.SecurityEventSearchCriteria;
import com.example.secrets_manager.tracing.CorrelationContext;
import com.example.secrets_manager.tracing.MissingCorrelationContextException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityEventLogService {

  private final SecurityEventLogRepository securityEventLogRepository;

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

  /**
   * Retrieves a paginated list of security events based on criteria. Restricted to administrators.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public Page<SecurityEventLogInfo> listSecurityEvents(
      SecurityEventSearchCriteria criteria, Pageable pageable) {
    // Force reverse chronological order by created time
    Pageable sortedPageable =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt"));

    Specification<SecurityEventLogEntity> spec =
        SecurityEventLogSpecifications.withCriteria(criteria);
    return securityEventLogRepository.findBy(
        spec, q -> q.as(SecurityEventLogInfo.class).page(sortedPageable));
  }

  /**
   * Retrieves the full details of a specific security event by its ID. Restricted to
   * administrators.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public SecurityEventLog getSecurityEventById(UUID id) {
    return securityEventLogRepository
        .findById(id)
        .map(SecurityEventLogEntityConverter::toModel)
        .orElseThrow(() -> new EntityNotFoundException("Security event not found with ID: " + id));
  }
}
