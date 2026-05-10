package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.data.converters.MasterKeyEntityConverter;
import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import com.example.secrets_manager.core.data.repositories.MasterKeyRepository;
import com.example.secrets_manager.core.data.repositories.MasterKeySpecifications;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.core.models.events.MasterKeyPromotedEvent;
import com.example.secrets_manager.core.models.search.MasterKeySearchCriteria;
import com.example.secrets_manager.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal service for high-privilege Master Key operations. This service should NEVER be injected
 * into Controllers.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InternalMasterKeyService {

  private final MasterKeyRepository masterKeyRepository;
  private final AuditService auditService;
  private final ApplicationEventPublisher eventPublisher;

  /** Lists master keys based on the provided search criteria. Internal use only. */
  @Transactional(readOnly = true)
  public List<MasterKey> listMasterKeys(MasterKeySearchCriteria criteria) {
    var spec = MasterKeySpecifications.withCriteria(criteria);
    return masterKeyRepository.findAll(spec).stream()
        .map(MasterKeyEntityConverter::toModel)
        .toList();
  }

  /**
   * Returns the highest version of master key present in the database. Returns zero if there are
   * none.
   */
  public int getHighestMasterKeyVersion() {
    return masterKeyRepository.findMaxVersion().orElse(0);
  }

  /** Retrieves metadata for a specific master key version. */
  @Transactional(readOnly = true)
  public MasterKey getMasterKeyMetadata(Integer version) {
    return masterKeyRepository
        .findById(version)
        .map(MasterKeyEntityConverter::toModel)
        .orElseThrow(() -> new EntityNotFoundException("Master key version not found: " + version));
  }

  /** Atomically retires existing active keys and registers a new active key. Internal use only. */
  @Transactional
  public MasterKey promoteNewKeyInternal(int version, String algorithm) {
    // Capture currently active versions before retirement for auditing and event
    final var retiredVersions =
        masterKeyRepository.findVersionsByStatus(MasterKeyState.ACTIVE.name());

    // Retire existing active keys
    masterKeyRepository.updateStatusByStatus(
        MasterKeyState.RETIRED.name(), MasterKeyState.ACTIVE.name());

    // Register the new key
    final var entity =
        MasterKeyEntity.builder()
            .version(version)
            .status(MasterKeyState.ACTIVE.name())
            .encryptAlgo(algorithm)
            .build();

    final var saved = masterKeyRepository.save(entity);
    log.info("Registered new Master Key v{} as ACTIVE using algorithm {}.", version, algorithm);

    // Audit the promotion event
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(SecurityUtils.getAuthenticatedUserId())
            .action(AuditAction.MASTER_KEY_PROMOTED)
            .targetMasterKeyVersion(version)
            .details(String.format("{\"retired_versions\": %s}", retiredVersions))
            .build());

    // Publish lifecycle event for side effects
    eventPublisher.publishEvent(new MasterKeyPromotedEvent(version, algorithm, retiredVersions));

    return MasterKeyEntityConverter.toModel(saved);
  }
}
