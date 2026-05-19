package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.components.MasterKeyProvider;
import com.example.secrets_manager.core.data.entities.SecretEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import com.example.secrets_manager.core.data.repositories.SecretGroupRepository;
import com.example.secrets_manager.core.data.repositories.SecretRepository;
import com.example.secrets_manager.core.models.*;
import com.example.secrets_manager.core.models.search.SecretSearchCriteria;
import com.example.secrets_manager.core.services.exceptions.SecretAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.SecretServiceException;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.impl.AesGcmSymmetricCipher;
import com.example.secrets_manager.crypto.impl.AesKw256SymmetricCipher;
import com.example.secrets_manager.crypto.impl.CryptographyServiceImpl;
import com.example.secrets_manager.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SecretServiceTest {

  @Mock private SecretRepository secretRepository;
  @Mock private SecretGroupRepository secretGroupRepository;
  @Mock private MasterKeyProvider masterKeyProvider;
  @Mock private InternalMasterKeyService internalMasterKeyService;
  @Mock private AuditService auditService;
  @Mock private ObjectMapper objectMapper;

  private CryptographyService cryptographyService;
  private SecretService secretService;
  private MockedStatic<SecurityUtils> mockedSecurityUtils;

  private final UUID userId = UUID.randomUUID();
  private final UUID groupId = UUID.randomUUID();
  private final byte[] masterKeyBytes = new byte[32];

  @BeforeEach
  void setUp() {
    // Use real crypto implementations for realistic testing
    cryptographyService =
        new CryptographyServiceImpl(
            List.of(),
            List.of(new AesGcmSymmetricCipher(), new AesKw256SymmetricCipher()),
            objectMapper);

    secretService =
        new SecretService(
            secretRepository,
            secretGroupRepository,
            cryptographyService,
            masterKeyProvider,
            internalMasterKeyService,
            auditService);

    mockedSecurityUtils = mockStatic(SecurityUtils.class);
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);

    // Initialize master key bytes
    for (int i = 0; i < 32; i++) masterKeyBytes[i] = (byte) i;
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtils.close();
  }

  @Test
  void createSecret_ShouldSucceed() {
    // Given
    var payload = new SecretCreationPayload("my-secret", "super-sensitive");
    var group = SecretGroupEntity.builder().id(groupId).encryptAlgo("AES-256-GCM").build();

    var mkMeta = MasterKey.builder().version(1).encryptAlgo("AES-KW-256").build();

    when(secretGroupRepository.findByIdAndDeletedAtIsNull(groupId)).thenReturn(Optional.of(group));
    when(masterKeyProvider.getActiveVersion()).thenReturn(1);
    when(internalMasterKeyService.getMasterKeyMetadata(1)).thenReturn(mkMeta);
    when(masterKeyProvider.getMasterKey(1)).thenReturn(masterKeyBytes);

    when(secretRepository.saveAndFlush(any(SecretEntity.class)))
        .thenAnswer(
            i -> {
              SecretEntity s = i.getArgument(0);
              s.setId(UUID.randomUUID());
              return s;
            });

    // When
    var result = secretService.createSecret(groupId, payload);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSecretName()).isEqualTo("my-secret");

    ArgumentCaptor<SecretEntity> captor = ArgumentCaptor.forClass(SecretEntity.class);
    verify(secretRepository).saveAndFlush(captor.capture());
    SecretEntity saved = captor.getValue();

    assertThat(saved.getDekVersion()).isEqualTo(1);
    assertThat(saved.getMasterKeyVersion()).isEqualTo(1);
    assertThat(saved.getDekCiphertext()).hasSize(40); // 32 bytes + 8 bytes ICV for AES-KW
    assertThat(saved.getValueCiphertext()).isNotEmpty();

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    var auditPayload = auditCaptor.getValue();
    assertThat(auditPayload.getAction()).isEqualTo(AuditAction.SECRET_CREATE);
    assertThat(auditPayload.getTargetGroupId()).isEqualTo(groupId);
    assertThat(auditPayload.getTargetSecretId()).isEqualTo(saved.getId());
  }

  @Test
  void createSecret_WithDuplicateName_ShouldThrowAlreadyExists() {
    // Given
    var payload = new SecretCreationPayload("duplicate", "val");
    var group = SecretGroupEntity.builder().id(groupId).encryptAlgo("AES-256-GCM").build();

    when(secretGroupRepository.findByIdAndDeletedAtIsNull(groupId)).thenReturn(Optional.of(group));
    when(masterKeyProvider.getActiveVersion()).thenReturn(1);
    when(masterKeyProvider.getMasterKey(eq(1))).thenReturn(masterKeyBytes);
    when(internalMasterKeyService.getMasterKeyMetadata(eq(1)))
        .thenReturn(MasterKey.builder().version(1).encryptAlgo("AES-KW-256").build());

    when(secretRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("uq_sm_secrets_active_group_name"));

    // When & Then
    assertThatThrownBy(() -> secretService.createSecret(groupId, payload))
        .isInstanceOf(SecretAlreadyExistsException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void getSecretValue_ShouldSucceed() {
    // Given
    String secretName = "top-secret";
    String plaintext = "my-password-123";

    // Create a realistic entity by using the real crypto service
    var group = SecretGroupEntity.builder().id(groupId).encryptAlgo("AES-256-GCM").build();

    byte[] dek = new byte[32];
    for (int i = 0; i < 32; i++) dek[i] = (byte) (32 - i);

    var valueEnvelope =
        cryptographyService.encrypt(plaintext.getBytes(StandardCharsets.UTF_8), dek, "AES-256-GCM");
    var dekEnvelope = cryptographyService.encrypt(dek, masterKeyBytes, "AES-KW-256");

    var entity =
        SecretEntity.builder()
            .id(UUID.randomUUID())
            .groupId(groupId)
            .secretName(secretName)
            .group(group)
            .valueCiphertext(valueEnvelope.getCiphertext())
            .valueNonce(valueEnvelope.getNonce())
            .valueAuthTag(valueEnvelope.getAuthTag())
            .dekCiphertext(dekEnvelope.getCiphertext())
            .masterKeyVersion(1)
            .build();

    when(secretRepository.findByGroupIdAndSecretNameAndDeletedAtIsNull(groupId, secretName))
        .thenReturn(Optional.of(entity));
    when(internalMasterKeyService.getMasterKeyMetadata(1))
        .thenReturn(MasterKey.builder().encryptAlgo("AES-KW-256").build());
    when(masterKeyProvider.getMasterKey(1)).thenReturn(masterKeyBytes);

    // When
    String result = secretService.getSecretValue(groupId, secretName);

    // Then
    assertThat(result).isEqualTo(plaintext);

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    var auditPayload = auditCaptor.getValue();
    assertThat(auditPayload.getAction()).isEqualTo(AuditAction.SECRET_READ);
    assertThat(auditPayload.getTargetGroupId()).isEqualTo(groupId);
    assertThat(auditPayload.getTargetSecretId()).isEqualTo(entity.getId());
  }

  @Test
  void getSecretValue_WithCorruptedData_ShouldThrowServiceException() {
    // Given
    String secretName = "corrupt";
    var entity =
        SecretEntity.builder()
            .masterKeyVersion(1)
            .dekCiphertext(new byte[40]) // Random noise
            .build();

    when(secretRepository.findByGroupIdAndSecretNameAndDeletedAtIsNull(any(), any()))
        .thenReturn(Optional.of(entity));
    when(internalMasterKeyService.getMasterKeyMetadata(any()))
        .thenReturn(MasterKey.builder().encryptAlgo("AES-KW-256").build());
    when(masterKeyProvider.getMasterKey(any())).thenReturn(masterKeyBytes);

    // When & Then
    assertThatThrownBy(() -> secretService.getSecretValue(groupId, secretName))
        .isInstanceOf(SecretServiceException.class)
        .hasMessageContaining("Failed to unwrap");
  }

  @Test
  void updateSecretValue_ShouldRotateDekAndSucceed() {
    // Given
    String secretName = "rotating-secret";
    var group = SecretGroupEntity.builder().id(groupId).encryptAlgo("AES-256-GCM").build();
    var entity =
        SecretEntity.builder()
            .id(UUID.randomUUID())
            .secretName(secretName)
            .group(group)
            .dekVersion(1)
            .build();

    when(secretRepository.findAndLockByGroupIdAndSecretNameAndDeletedAtIsNull(groupId, secretName))
        .thenReturn(Optional.of(entity));
    when(masterKeyProvider.getActiveVersion()).thenReturn(2);
    when(internalMasterKeyService.getMasterKeyMetadata(2))
        .thenReturn(MasterKey.builder().encryptAlgo("AES-KW-256").build());
    when(masterKeyProvider.getMasterKey(2)).thenReturn(masterKeyBytes);
    when(secretRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    // When
    var result =
        secretService.updateSecretValue(
            groupId, secretName, new SecretValueUpdatePayload("new-val"));

    // Then
    assertThat(result.getDekVersion()).isEqualTo(2);
    verify(secretRepository).save(any());

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    var auditPayload = auditCaptor.getValue();
    assertThat(auditPayload.getAction()).isEqualTo(AuditAction.SECRET_UPDATE);
    assertThat(auditPayload.getTargetGroupId()).isEqualTo(groupId);
    assertThat(auditPayload.getTargetSecretId()).isEqualTo(entity.getId());
  }

  @Test
  void deleteSecret_ShouldPerformSoftDelete() {
    // Given
    String name = "gone";
    var entity = SecretEntity.builder().id(UUID.randomUUID()).secretName(name).build();
    when(secretRepository.findByGroupIdAndSecretNameAndDeletedAtIsNull(groupId, name))
        .thenReturn(Optional.of(entity));

    // When
    secretService.deleteSecret(groupId, name);

    // Then
    verify(secretRepository).delete(entity);

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    var auditPayload = auditCaptor.getValue();
    assertThat(auditPayload.getAction()).isEqualTo(AuditAction.SECRET_DELETE);
    assertThat(auditPayload.getTargetGroupId()).isEqualTo(groupId);
    assertThat(auditPayload.getTargetSecretId()).isEqualTo(entity.getId());
  }

  @Test
  void listSecrets_ShouldReturnMappedPage() {
    // Given
    var pageable = PageRequest.of(0, 10);
    var entity = SecretEntity.builder().secretName("s1").build();
    when(secretRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    // When
    var result = secretService.listSecrets(groupId, new SecretSearchCriteria(), pageable);

    // Then
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getSecretName()).isEqualTo("s1");
  }
}
