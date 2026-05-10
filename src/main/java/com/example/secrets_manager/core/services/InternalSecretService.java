package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.components.MasterKeyProvider;
import com.example.secrets_manager.core.data.entities.SecretEntity;
import com.example.secrets_manager.core.data.repositories.SecretRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.services.exceptions.SecretServiceException;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import com.example.secrets_manager.security.SecurityUtils;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal service for high-privilege secret operations. Acts as the core cryptographic engine for
 * maintenance tasks such as master key upgrades.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InternalSecretService {

  private final SecretRepository secretRepository;
  private final CryptographyService cryptographyService;
  private final MasterKeyProvider masterKeyProvider;
  private final InternalMasterKeyService internalMasterKeyService;
  private final AuditService auditService;

  /**
   * Upgrades a secret's protection to a new master key version. Performs a re-wrap of the Data
   * Encryption Key (DEK) without re-encrypting the data itself.
   *
   * @param secretId The ID of the secret to upgrade.
   * @param targetMkVersion The target master key version.
   * @param taskId Task ID for transactional fencing.
   */
  @Transactional
  public void upgradeMasterKey(UUID secretId, int targetMkVersion, UUID taskId) {
    final var entity =
        secretRepository
            .findAndLockByIdAndDeletedAtIsNull(secretId)
            .orElseThrow(() -> new EntityNotFoundException("Secret not found: " + secretId));

    // Idempotency Check: Avoid redundant re-wraps
    if (entity.getMasterKeyVersion() >= targetMkVersion) {
      log.debug(
          "Secret {} is already using master key v{}. Skipping upgrade.",
          secretId,
          targetMkVersion);
      return;
    }

    // Unwrap DEK with current Master Key
    final var oldMkMeta =
        internalMasterKeyService.getMasterKeyMetadata(entity.getMasterKeyVersion());
    final var oldMkBytes = masterKeyProvider.getMasterKey(entity.getMasterKeyVersion());

    final var currentWrappedDek =
        new EncryptedData(
            entity.getDekCiphertext(),
            entity.getDekNonce(),
            entity.getDekAuthTag(),
            oldMkMeta.getEncryptAlgo());

    byte[] dek;
    try {
      dek = cryptographyService.decrypt(currentWrappedDek, oldMkBytes);
    } catch (CryptoOperationException e) {
      log.error(
          "Failed to unwrap DEK for secret {} using master key v{}",
          secretId,
          entity.getMasterKeyVersion(),
          e);
      throw new SecretServiceException("Cryptographic failure during master key upgrade", e);
    }

    // Re-wrap DEK with the Target Master Key
    final var newMkMeta = internalMasterKeyService.getMasterKeyMetadata(targetMkVersion);
    final var newMkBytes = masterKeyProvider.getMasterKey(targetMkVersion);

    final var upgradedDekEnvelope =
        cryptographyService.encrypt(dek, newMkBytes, newMkMeta.getEncryptAlgo());

    // Update persistence state
    int updated =
        secretRepository.updateSecretFenced(
            secretId,
            taskId,
            TaskUtils.WORKER_ID,
            upgradedDekEnvelope.getCiphertext(),
            upgradedDekEnvelope.getNonce(),
            upgradedDekEnvelope.getAuthTag(),
            targetMkVersion);

    if (updated == 0) {
      throw new TaskAssignmentEvictedException(taskId);
    }

    // Record the maintenance event
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(SecurityUtils.getAuthenticatedUserId())
            .action(AuditAction.SECRET_MASTER_KEY_UPGRADED)
            .targetGroupId(entity.getGroupId())
            .targetSecretId(entity.getId())
            .details(
                String.format(
                    "{\"from_version\": %d, \"to_version\": %d}",
                    oldMkMeta.getVersion(), targetMkVersion))
            .build());

    log.info("Successfully upgraded master key for secret {} to v{}", secretId, targetMkVersion);
  }
}
