package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.components.MasterKeyProvider;
import com.example.secrets_manager.core.data.entities.SecretEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import com.example.secrets_manager.core.data.repositories.SecretRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.impl.AesGcmSymmetricCipher;
import com.example.secrets_manager.crypto.impl.AesKw256SymmetricCipher;
import com.example.secrets_manager.crypto.impl.CryptographyServiceImpl;
import com.example.secrets_manager.security.WithMockAppUser;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import com.example.secrets_manager.tracing.WithCorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalSecretServiceTest {

  @Mock private SecretRepository secretRepository;
  @Mock private MasterKeyProvider masterKeyProvider;
  @Mock private InternalMasterKeyService internalMasterKeyService;
  @Mock private AuditService auditService;

  private CryptographyService cryptographyService;
  private InternalSecretService service;

  @BeforeEach
  void setUp() {
    // Initialize real cryptography service with required ciphers and wrap it in a Spy
    var realCrypto =
        new CryptographyServiceImpl(
            List.of(),
            List.of(new AesGcmSymmetricCipher(), new AesKw256SymmetricCipher()),
            new ObjectMapper());
    cryptographyService = spy(realCrypto);

    service =
        new InternalSecretService(
            secretRepository,
            cryptographyService,
            masterKeyProvider,
            internalMasterKeyService,
            auditService);
  }

  @Test
  @WithMockAppUser
  @WithCorrelationId
  void upgradeMasterKey_ShouldThrowEvictedException_WhenHardFenceFails() throws Exception {
    // Given
    UUID secretId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();
    int targetVersion = 2;

    byte[] oldMkBytes = new byte[32]; // AES-256
    byte[] newMkBytes = new byte[32];
    byte[] rawDek = new byte[32];

    // Create a REAL valid encrypted DEK envelope using the old master key
    var dekEnvelope = cryptographyService.encrypt(rawDek, oldMkBytes, "AES-KW-256");

    SecretGroupEntity group = SecretGroupEntity.builder().encryptAlgo("AES-256-GCM").build();
    SecretEntity entity =
        SecretEntity.builder()
            .id(secretId)
            .masterKeyVersion(1)
            .group(group)
            .dekCiphertext(dekEnvelope.getCiphertext())
            .dekNonce(dekEnvelope.getNonce())
            .dekAuthTag(dekEnvelope.getAuthTag())
            .build();

    when(secretRepository.findAndLockByIdAndDeletedAtIsNull(secretId))
        .thenReturn(Optional.of(entity));

    when(internalMasterKeyService.getMasterKeyMetadata(1))
        .thenReturn(MasterKey.builder().version(1).encryptAlgo("AES-KW-256").build());
    when(internalMasterKeyService.getMasterKeyMetadata(2))
        .thenReturn(MasterKey.builder().version(2).encryptAlgo("AES-KW-256").build());

    when(masterKeyProvider.getMasterKey(1)).thenReturn(oldMkBytes);
    when(masterKeyProvider.getMasterKey(2)).thenReturn(newMkBytes);

    when(secretRepository.updateSecretFenced(
            eq(secretId), eq(taskId), any(), any(), any(), any(), eq(targetVersion)))
        .thenReturn(0);

    // When & Then
    assertThatThrownBy(() -> service.upgradeMasterKey(secretId, targetVersion, taskId))
        .isInstanceOf(TaskAssignmentEvictedException.class);

    verify(auditService, never()).save(any());
  }

  @Test
  @WithMockAppUser
  @WithCorrelationId
  void upgradeMasterKey_ShouldSucceed_WhenEverythingIsValid() throws Exception {
    // Given
    UUID secretId = UUID.randomUUID();
    UUID taskId = UUID.randomUUID();
    int targetVersion = 2;

    byte[] oldMkBytes = new byte[32];
    byte[] newMkBytes = new byte[32];
    byte[] rawDek = new byte[32];

    var dekEnvelope = cryptographyService.encrypt(rawDek, oldMkBytes, "AES-KW-256");

    SecretGroupEntity group = SecretGroupEntity.builder().encryptAlgo("AES-256-GCM").build();
    SecretEntity entity =
        SecretEntity.builder()
            .id(secretId)
            .groupId(UUID.randomUUID())
            .masterKeyVersion(1)
            .group(group)
            .dekCiphertext(dekEnvelope.getCiphertext())
            .dekNonce(dekEnvelope.getNonce())
            .dekAuthTag(dekEnvelope.getAuthTag())
            .build();

    when(secretRepository.findAndLockByIdAndDeletedAtIsNull(secretId))
        .thenReturn(Optional.of(entity));

    when(internalMasterKeyService.getMasterKeyMetadata(1))
        .thenReturn(MasterKey.builder().version(1).encryptAlgo("AES-KW-256").build());
    when(internalMasterKeyService.getMasterKeyMetadata(2))
        .thenReturn(MasterKey.builder().version(2).encryptAlgo("AES-KW-256").build());

    when(masterKeyProvider.getMasterKey(1)).thenReturn(oldMkBytes);
    when(masterKeyProvider.getMasterKey(2)).thenReturn(newMkBytes);

    when(secretRepository.updateSecretFenced(
            eq(secretId), eq(taskId), any(), any(), any(), any(), eq(targetVersion)))
        .thenReturn(1);

    // When
    service.upgradeMasterKey(secretId, targetVersion, taskId);

    // Then
    verify(secretRepository)
        .updateSecretFenced(
            eq(secretId),
            eq(taskId),
            eq(TaskUtils.WORKER_ID),
            any(),
            any(),
            any(),
            eq(targetVersion));
    verify(auditService)
        .save(argThat(p -> p.getAction() == AuditAction.SECRET_MASTER_KEY_UPGRADED));
  }

  @Test
  @WithMockAppUser
  @WithCorrelationId
  void upgradeMasterKey_ShouldSkip_WhenAlreadyAtTargetVersion() throws Exception {
    // Given
    UUID secretId = UUID.randomUUID();
    int targetVersion = 2;

    SecretEntity entity = SecretEntity.builder().id(secretId).masterKeyVersion(2).build();

    when(secretRepository.findAndLockByIdAndDeletedAtIsNull(secretId))
        .thenReturn(Optional.of(entity));

    // When
    service.upgradeMasterKey(secretId, targetVersion, null);

    // Then
    verify(cryptographyService, never()).decrypt(any(), any());
    verify(secretRepository, never()).save(any());
    verify(auditService, never()).save(any());
  }
}
