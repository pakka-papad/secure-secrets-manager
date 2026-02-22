package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.AuditLogEntityConverter;
import com.example.secrets_manager.core.data.entities.AuditLogEntity;
import com.example.secrets_manager.core.data.repositories.AuditLogRepository;
import com.example.secrets_manager.core.models.AuditLog;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.SystemLockName;
import com.example.secrets_manager.crypto.CryptographyService;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

  private final AuditLogRepository auditLogRepository;
  private final SystemLockService systemLockService;
  private final CryptographyService cryptographyService;

  @Autowired
  public AuditService(
      AuditLogRepository auditLogRepository,
      SystemLockService systemLockService,
      CryptographyService cryptographyService) {
    this.auditLogRepository = auditLogRepository;
    this.systemLockService = systemLockService;
    this.cryptographyService = cryptographyService;
  }

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
   */
  @Transactional
  public AuditLog save(AuditLogPayload payload) {
    // --- 1. Acquire exclusive lock to serialize chain access ---
    systemLockService.acquireExclusiveLock(SystemLockName.AUDIT_LOG_CHAIN);

    // --- 2. Fetch the latest record to determine the previous hash ---
    // This now expects the genesis record to exist and will fail if it doesn't.
    AuditLogEntity lastLog =
        auditLogRepository
            .findTopByOrderBySeqIdDesc()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Audit log genesis record is missing. The chain cannot be continued."));

    // --- 3. Construct the full AuditLog model from the payload ---
    AuditLog logData =
        AuditLog.builder()
            .actorUserId(payload.getActorUserId())
            .action(payload.getAction())
            .targetUserId(payload.getTargetUserId())
            .targetGroupId(payload.getTargetGroupId())
            .targetSecretId(payload.getTargetSecretId())
            .targetMasterKeyVersion(payload.getTargetMasterKeyVersion())
            .details(payload.getDetails())
            .createdAt(Instant.now())
            .prevHash(lastLog.getDataHash())
            .build();

    // --- 4. Calculate the data hash for the NEW record ---
    byte[] dataHash = cryptographyService.createDataHash(logData);
    logData.setDataHash(dataHash);

    // --- 5. Convert to entity and save ---
    AuditLogEntity entityToSave = AuditLogEntityConverter.fromModel(logData);
    AuditLogEntity savedEntity = auditLogRepository.save(entityToSave);

    return AuditLogEntityConverter.toModel(savedEntity);
  }
}
