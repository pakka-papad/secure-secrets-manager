package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import com.example.secrets_manager.core.data.repositories.MasterKeyRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.core.models.events.MasterKeyPromotedEvent;
import com.example.secrets_manager.core.models.search.MasterKeySearchCriteria;
import com.example.secrets_manager.security.WithMockAppUser;
import com.example.secrets_manager.tracing.WithCorrelationId;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class InternalMasterKeyServiceTest {

  @Mock private MasterKeyRepository masterKeyRepository;
  @Mock private AuditService auditService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private InternalMasterKeyService internalMasterKeyService;

  @Test
  void listMasterKeys_ShouldReturnConvertedList() {
    // Given
    var criteria = MasterKeySearchCriteria.builder().build();
    var entity =
        MasterKeyEntity.builder()
            .version(1)
            .status(MasterKeyState.ACTIVE.name())
            .encryptAlgo("AES-256-GCM")
            .build();

    when(masterKeyRepository.findAll(any(Specification.class))).thenReturn(List.of(entity));

    // When
    var result = internalMasterKeyService.listMasterKeys(criteria);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getVersion()).isEqualTo(1);
    assertThat(result.get(0).getStatus()).isEqualTo(MasterKeyState.ACTIVE);
  }

  @Test
  void getHighestMasterKeyVersion_ShouldReturnMaxFromDb() {
    // Given
    when(masterKeyRepository.findMaxVersion()).thenReturn(Optional.of(10));

    // When
    int result = internalMasterKeyService.getHighestMasterKeyVersion();

    // Then
    assertThat(result).isEqualTo(10);
  }

  @Test
  void getHighestMasterKeyVersion_WhenNoKeys_ShouldReturnZero() {
    // Given
    when(masterKeyRepository.findMaxVersion()).thenReturn(Optional.empty());

    // When
    int result = internalMasterKeyService.getHighestMasterKeyVersion();

    // Then
    assertThat(result).isZero();
  }

  @Test
  void getMasterKeyMetadata_ShouldReturnModel_WhenExists() {
    // Given
    int version = 1;
    var entity =
        MasterKeyEntity.builder()
            .version(version)
            .status(MasterKeyState.ACTIVE.name())
            .encryptAlgo("AES-256-GCM")
            .build();

    when(masterKeyRepository.findById(version)).thenReturn(Optional.of(entity));

    // When
    var result = internalMasterKeyService.getMasterKeyMetadata(version);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getVersion()).isEqualTo(version);
    assertThat(result.getEncryptAlgo()).isEqualTo("AES-256-GCM");
  }

  @Test
  void getMasterKeyMetadata_ShouldThrowEntityNotFound_WhenDoesNotExist() {
    // Given
    int version = 999;
    when(masterKeyRepository.findById(version)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> internalMasterKeyService.getMasterKeyMetadata(version))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("not found");
  }

  @Test
  @WithMockAppUser
  @WithCorrelationId
  void promoteNewKeyInternal_ShouldRetireOldAuditAndPublishEvent() {
    // Given
    int newVersion = 2;
    String algo = "CHACHA20-POLY1305";
    List<Integer> currentlyActive = List.of(1);

    when(masterKeyRepository.findVersionsByStatus(MasterKeyState.ACTIVE.name()))
        .thenReturn(currentlyActive);
    when(masterKeyRepository.save(any(MasterKeyEntity.class))).thenAnswer(i -> i.getArgument(0));

    // When
    var result = internalMasterKeyService.promoteNewKeyInternal(newVersion, algo);

    // Then
    verify(masterKeyRepository)
        .updateStatusByStatus(MasterKeyState.RETIRED.name(), MasterKeyState.ACTIVE.name());

    ArgumentCaptor<MasterKeyEntity> captor = ArgumentCaptor.forClass(MasterKeyEntity.class);
    verify(masterKeyRepository).save(captor.capture());
    assertThat(captor.getValue().getVersion()).isEqualTo(newVersion);
    assertThat(captor.getValue().getStatus()).isEqualTo(MasterKeyState.ACTIVE.name());
    assertThat(captor.getValue().getEncryptAlgo()).isEqualTo(algo);

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.MASTER_KEY_PROMOTED);
    assertThat(auditCaptor.getValue().getTargetMasterKeyVersion()).isEqualTo(newVersion);
    assertThat(auditCaptor.getValue().getDetails()).contains("1");

    ArgumentCaptor<MasterKeyPromotedEvent> eventCaptor =
        ArgumentCaptor.forClass(MasterKeyPromotedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().newVersion()).isEqualTo(newVersion);
    assertThat(eventCaptor.getValue().algorithm()).isEqualTo(algo);
    assertThat(eventCaptor.getValue().retiredVersions()).isEqualTo(currentlyActive);

    assertThat(result.getVersion()).isEqualTo(newVersion);
    assertThat(result.getStatus()).isEqualTo(MasterKeyState.ACTIVE);
  }
}
