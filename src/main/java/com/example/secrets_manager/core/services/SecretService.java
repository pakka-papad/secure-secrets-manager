package com.example.secrets_manager.core.services;

import com.example.secrets_manager.core.components.MasterKeyProvider;
import com.example.secrets_manager.core.data.converters.SecretEntityConverter;
import com.example.secrets_manager.core.data.entities.SecretEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import com.example.secrets_manager.core.data.repositories.SecretGroupRepository;
import com.example.secrets_manager.core.data.repositories.SecretRepository;
import com.example.secrets_manager.core.data.repositories.SecretSpecifications;
import com.example.secrets_manager.core.models.*;
import com.example.secrets_manager.core.models.search.SecretSearchCriteria;
import com.example.secrets_manager.core.services.exceptions.SecretAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.SecretServiceException;
import com.example.secrets_manager.core.utils.PaginationUtils;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import com.example.secrets_manager.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Slf4j
@RequiredArgsConstructor
@Validated
public class SecretService {

  private final SecretRepository secretRepository;
  private final SecretGroupRepository secretGroupRepository;
  private final CryptographyService cryptographyService;
  private final MasterKeyProvider masterKeyProvider;
  private final InternalMasterKeyService internalMasterKeyService;
  private final AuditService auditService;

  /** Creates a new secret within a group. */
  @Transactional
  @PreAuthorize("@groupAuth.canWrite(principal, #groupId)")
  public Secret createSecret(UUID groupId, @Valid @NotNull SecretCreationPayload payload) {
    final var group = getGroupOrThrow(groupId);

    // 1. Generate DEK using the group's algorithm
    final var dek = cryptographyService.generateKey(group.getEncryptAlgo());

    // 2. Encrypt value with DEK (Group algorithm)
    final var valueEnvelope =
        cryptographyService.encrypt(
            payload.getPlaintextValue().getBytes(StandardCharsets.UTF_8),
            dek.getEncoded(),
            group.getEncryptAlgo());

    // 3. Wrap DEK with Active Master Key (AES-KW-256)
    final var mkVersion = masterKeyProvider.getActiveVersion();
    final var mkMeta = internalMasterKeyService.getMasterKeyMetadata(mkVersion);
    final var mkBytes = masterKeyProvider.getMasterKey(mkVersion);

    final var dekEnvelope =
        cryptographyService.encrypt(dek.getEncoded(), mkBytes, mkMeta.getEncryptAlgo());

    // 4. Create entity
    final var entity =
        SecretEntity.builder()
            .groupId(groupId)
            .secretName(payload.getName())
            .valueCiphertext(valueEnvelope.getCiphertext())
            .valueNonce(valueEnvelope.getNonce())
            .valueAuthTag(valueEnvelope.getAuthTag())
            .dekCiphertext(dekEnvelope.getCiphertext())
            .dekNonce(dekEnvelope.getNonce())
            .dekAuthTag(dekEnvelope.getAuthTag())
            .dekVersion(1) // Initial DEK version
            .masterKeyVersion(mkVersion)
            .build();

    SecretEntity saved;
    try {
      saved = secretRepository.saveAndFlush(entity);
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("uq_sm_secrets_active_group_name")) {
        throw new SecretAlreadyExistsException(
            String.format("Secret with name '%s' already exists in this group.", payload.getName()),
            e);
      }
      throw new SecretServiceException(
          "Failed to create secret due to data integrity violation.", e);
    }

    audit(AuditAction.SECRET_CREATE, groupId, saved.getId());
    return SecretEntityConverter.toModel(saved);
  }

  /** Retrieves the decrypted plaintext value of a secret. */
  @Transactional(readOnly = true)
  @PreAuthorize("@groupAuth.canRead(principal, #groupId)")
  public String getSecretValue(UUID groupId, String secretName) {
    final var entity = getSecretOrThrow(groupId, secretName);

    // 1. Load Master Key Metadata and Bytes
    final var mkMeta = internalMasterKeyService.getMasterKeyMetadata(entity.getMasterKeyVersion());
    final var mkBytes = masterKeyProvider.getMasterKey(entity.getMasterKeyVersion());

    // 2. Unwrap the DEK
    final var wrappedDek =
        new EncryptedData(
            entity.getDekCiphertext(),
            entity.getDekNonce(),
            entity.getDekAuthTag(),
            mkMeta.getEncryptAlgo());

    byte[] dek;
    try {
      dek = cryptographyService.decrypt(wrappedDek, mkBytes);
    } catch (CryptoOperationException e) {
      log.error("Failed to unwrap DEK for secret {} in group {}", secretName, groupId, e);
      throw new SecretServiceException(
          "Failed to unwrap Data Encryption Key. Master Key may be incorrect or missing.", e);
    }

    // 3. Decrypt the Secret Value
    final var wrappedValue =
        new EncryptedData(
            entity.getValueCiphertext(),
            entity.getValueNonce(),
            entity.getValueAuthTag(),
            entity.getGroup().getEncryptAlgo());

    byte[] plaintext;
    try {
      plaintext = cryptographyService.decrypt(wrappedValue, dek);
    } catch (CryptoOperationException e) {
      log.error("Failed to decrypt value for secret {} in group {}", secretName, groupId, e);
      throw new SecretServiceException(
          "Failed to decrypt secret value. Data may be corrupt or tampered with.", e);
    }

    audit(AuditAction.SECRET_READ, groupId, entity.getId());
    return new String(plaintext, StandardCharsets.UTF_8);
  }

  /** Retrieves metadata for a specific secret. */
  @Transactional(readOnly = true)
  @PreAuthorize("@groupAuth.canRead(principal, #groupId)")
  public Secret getSecretMetadata(UUID groupId, String secretName) {
    final var entity = getSecretOrThrow(groupId, secretName);
    return SecretEntityConverter.toModel(entity);
  }

  /** Updates the value of an existing secret. */
  @Transactional
  @PreAuthorize("@groupAuth.canWrite(principal, #groupId)")
  public Secret updateSecretValue(
      UUID groupId, String secretName, SecretValueUpdatePayload payload) {
    final var entity =
        secretRepository
            .findAndLockByGroupIdAndSecretNameAndDeletedAtIsNull(groupId, secretName)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format("Secret '%s' not found in group %s", secretName, groupId)));

    // For updates, we generate a NEW DEK (DEK rotation on every write)
    final var newDek = cryptographyService.generateKey(entity.getGroup().getEncryptAlgo());

    final var valueEnvelope =
        cryptographyService.encrypt(
            payload.getPlaintextValue().getBytes(StandardCharsets.UTF_8),
            newDek.getEncoded(),
            entity.getGroup().getEncryptAlgo());

    final var mkVersion = masterKeyProvider.getActiveVersion();
    final var mkMeta = internalMasterKeyService.getMasterKeyMetadata(mkVersion);
    final var mkBytes = masterKeyProvider.getMasterKey(mkVersion);

    final var dekEnvelope =
        cryptographyService.encrypt(newDek.getEncoded(), mkBytes, mkMeta.getEncryptAlgo());

    entity.setValueCiphertext(valueEnvelope.getCiphertext());
    entity.setValueNonce(valueEnvelope.getNonce());
    entity.setValueAuthTag(valueEnvelope.getAuthTag());
    entity.setDekCiphertext(dekEnvelope.getCiphertext());
    entity.setDekNonce(dekEnvelope.getNonce());
    entity.setDekAuthTag(dekEnvelope.getAuthTag());
    entity.setMasterKeyVersion(mkVersion);
    entity.setDekVersion(entity.getDekVersion() + 1);
    entity.setModifiedAt(Instant.now());

    final var saved = secretRepository.save(entity);
    audit(AuditAction.SECRET_UPDATE, groupId, saved.getId());
    return SecretEntityConverter.toModel(saved);
  }

  /** Soft-deletes a secret. */
  @Transactional
  @PreAuthorize("@groupAuth.canDelete(principal, #groupId)")
  public void deleteSecret(UUID groupId, String secretName) {
    final var entity = getSecretOrThrow(groupId, secretName);

    // Perform Soft Delete
    entity.setDeletedAt(Instant.now());
    secretRepository.save(entity);

    audit(AuditAction.SECRET_DELETE, groupId, entity.getId());
  }

  /**
   * Lists metadata for active secrets in a group with pagination and filtering.
   *
   * @param groupId The ID of the group.
   * @param criteria The {@link SecretSearchCriteria} containing filters.
   * @param pageable The {@link Pageable} object for pagination and sorting.
   * @return A {@link Page} of {@link Secret} models.
   */
  @Transactional(readOnly = true)
  @PreAuthorize("@groupAuth.canRead(principal, #groupId)")
  public Page<Secret> listSecrets(UUID groupId, SecretSearchCriteria criteria, Pageable pageable) {
    PaginationUtils.validateSort(pageable, SecretEntity.ALLOWED_SORT_FIELDS);
    final var spec = SecretSpecifications.withCriteria(groupId, criteria);
    return secretRepository.findAll(spec, pageable).map(SecretEntityConverter::toModel);
  }

  private SecretGroupEntity getGroupOrThrow(UUID groupId) {
    return secretGroupRepository
        .findByIdAndDeletedAtIsNull(groupId)
        .orElseThrow(() -> new EntityNotFoundException("Secret group not found: " + groupId));
  }

  private SecretEntity getSecretOrThrow(UUID groupId, String name) {
    return secretRepository
        .findByGroupIdAndSecretNameAndDeletedAtIsNull(groupId, name)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    String.format("Secret '%s' not found in group %s", name, groupId)));
  }

  private void audit(AuditAction action, UUID groupId, UUID secretId) {
    auditService.save(
        AuditLogPayload.builder()
            .actorUserId(SecurityUtils.getAuthenticatedUserId())
            .action(action)
            .targetGroupId(groupId)
            .targetSecretId(secretId)
            .build());
  }
}
