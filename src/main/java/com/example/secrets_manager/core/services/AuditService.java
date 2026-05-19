package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.AuditLogEntityConverter;
import com.example.secrets_manager.core.data.entities.AuditLogEntity;
import com.example.secrets_manager.core.data.repositories.AuditLogInfo;
import com.example.secrets_manager.core.data.repositories.AuditLogRepository;
import com.example.secrets_manager.core.data.repositories.AuditLogSpecifications;
import com.example.secrets_manager.core.models.AuditLog;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.SystemLockName;
import com.example.secrets_manager.core.models.search.AuditLogSearchCriteria;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.tracing.CorrelationContext;
import com.example.secrets_manager.tracing.MissingCorrelationContextException;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

  private final AuditLogRepository auditLogRepository;
  private final SystemLockService systemLockService;
  private final CryptographyService cryptographyService;

  /**
   * Persists an audit log event from a payload, handling cryptographic chaining and locking. This
   * method assumes a genesis record already exists and will throw an exception if the audit log is
   * empty.
   *
   * <p>This method should be called from within an existing @Transactional block in a calling
   * service.
   *
   * @param payload The AuditLogPayload containing the details of the audit event.
   * @return The persisted AuditLog model, complete with its seqId and calculated hashes.
   * @throws MissingCorrelationContextException if no correlation ID is found in payload or context.
   */
  @Transactional
  public AuditLog save(AuditLogPayload payload) {
    // Resolve Correlation ID (Strict Enforcement)
    final var correlationId =
        payload.getCorrelationId() != null
            ? payload.getCorrelationId()
            : CorrelationContext.get()
                .orElseThrow(
                    () ->
                        new MissingCorrelationContextException(
                            "Audit log cannot be saved without a Correlation ID. Traceability is mandatory."));

    // Acquire exclusive lock to serialize chain access
    systemLockService.acquireExclusiveLock(SystemLockName.AUDIT_LOG_CHAIN);

    // Fetch the latest record to determine the previous hash
    // This now expects the genesis record to exist and will fail if it doesn't.
    AuditLogEntity lastLog =
        auditLogRepository
            .findTopByOrderBySeqIdDesc()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Audit log genesis record is missing. The chain cannot be continued."));

    // Construct the full AuditLog model from the payload
    AuditLog logData =
        AuditLog.builder()
            .actorUserId(payload.getActorUserId())
            .action(payload.getAction())
            .correlationId(correlationId)
            .targetUserId(payload.getTargetUserId())
            .targetGroupId(payload.getTargetGroupId())
            .targetSecretId(payload.getTargetSecretId())
            .targetMasterKeyVersion(payload.getTargetMasterKeyVersion())
            .details(payload.getDetails())
            .createdAt(Instant.now())
            .prevHash(lastLog.getDataHash())
            .build();

    // Calculate the data hash for the NEW record
    byte[] dataHash = cryptographyService.createDataHash(logData);
    logData.setDataHash(dataHash);

    // Convert to entity and save
    AuditLogEntity entityToSave = AuditLogEntityConverter.fromModel(logData);
    AuditLogEntity savedEntity = auditLogRepository.save(entityToSave);

    return AuditLogEntityConverter.toModel(savedEntity);
  }

  /** Retrieves a paginated list of audit logs based on criteria. Restricted to administrators. */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public Page<AuditLogInfo> listAuditLogs(AuditLogSearchCriteria criteria, Pageable pageable) {
    Pageable sortedPageable =
        PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, AuditLogEntity.COL_SEQ_ID));

    Specification<AuditLogEntity> spec = AuditLogSpecifications.withCriteria(criteria);
    return auditLogRepository.findBy(spec, q -> q.as(AuditLogInfo.class).page(sortedPageable));
  }

  /**
   * Retrieves the full details of a specific audit log by its sequence ID. Restricted to
   * administrators.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("hasRole('ADMIN')")
  public AuditLog getAuditLogById(Long seqId) {
    return auditLogRepository
        .findById(seqId)
        .map(AuditLogEntityConverter::toModel)
        .orElseThrow(
            () -> new EntityNotFoundException("Audit log not found with sequence ID: " + seqId));
  }
}
