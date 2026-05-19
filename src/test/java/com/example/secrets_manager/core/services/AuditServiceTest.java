package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.AuditLogEntity;
import com.example.secrets_manager.core.data.repositories.AuditLogInfo;
import com.example.secrets_manager.core.data.repositories.AuditLogRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLog;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.SystemLockName;
import com.example.secrets_manager.core.models.search.AuditLogSearchCriteria;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.tracing.CorrelationContext;
import com.example.secrets_manager.tracing.MissingCorrelationContextException;
import com.example.secrets_manager.tracing.WithCorrelationId;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

  @Mock private AuditLogRepository auditLogRepository;
  @Mock private SystemLockService systemLockService;
  @Mock private CryptographyService cryptographyService;

  @InjectMocks private AuditService auditService;

  @Test
  @WithCorrelationId
  void save_ShouldChainLogCorrectly() {
    // Given
    UUID correlationId = CorrelationContext.get().orElseThrow();

    UUID actorId = UUID.randomUUID();
    AuditLogPayload payload =
        AuditLogPayload.builder().actorUserId(actorId).action(AuditAction.USER_CREATE).build();

    byte[] lastDataHash = new byte[] {1, 2, 3};
    AuditLogEntity lastLog = AuditLogEntity.builder().seqId(100L).dataHash(lastDataHash).build();

    when(auditLogRepository.findTopByOrderBySeqIdDesc()).thenReturn(Optional.of(lastLog));

    byte[] newDataHash = new byte[] {4, 5, 6};
    when(cryptographyService.createDataHash(any())).thenReturn(newDataHash);

    when(auditLogRepository.save(any()))
        .thenAnswer(
            invocation -> {
              AuditLogEntity entity = invocation.getArgument(0);
              entity.setSeqId(101L);
              return entity;
            });

    // When
    AuditLog result = auditService.save(payload);

    // Then
    assertThat(result.getSeqId()).isEqualTo(101L);
    assertThat(result.getCorrelationId()).isEqualTo(correlationId);
    assertThat(result.getPrevHash()).isEqualTo(lastDataHash);
    assertThat(result.getDataHash()).isEqualTo(newDataHash);

    verify(systemLockService).acquireExclusiveLock(SystemLockName.AUDIT_LOG_CHAIN);

    ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
    verify(cryptographyService).createDataHash(logCaptor.capture());
    assertThat(logCaptor.getValue().getPrevHash()).isEqualTo(lastDataHash);
    assertThat(logCaptor.getValue().getCorrelationId()).isEqualTo(correlationId);
  }

  @Test
  @WithCorrelationId
  void save_WithExplicitCorrelationIdInPayload_ShouldUsePayloadId() {
    // Given
    UUID payloadCid = UUID.randomUUID();

    AuditLogPayload payload =
        AuditLogPayload.builder()
            .actorUserId(UUID.randomUUID())
            .action(AuditAction.USER_CREATE)
            .correlationId(payloadCid)
            .build();

    AuditLogEntity lastLog = AuditLogEntity.builder().seqId(100L).dataHash(new byte[0]).build();
    when(auditLogRepository.findTopByOrderBySeqIdDesc()).thenReturn(Optional.of(lastLog));
    when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // When
    AuditLog result = auditService.save(payload);

    // Then
    assertThat(result.getCorrelationId()).isEqualTo(payloadCid);
  }

  @Test
  void save_WithoutCorrelationId_ShouldThrowMissingCorrelationContextException() {
    // Given - ensure context is empty
    CorrelationContext.clear();
    AuditLogPayload payload = new AuditLogPayload();

    // When & Then
    assertThatThrownBy(() -> auditService.save(payload))
        .isInstanceOf(MissingCorrelationContextException.class)
        .hasMessageContaining("without a Correlation ID");
  }

  @Test
  @WithCorrelationId
  void save_WhenGenesisMissing_ShouldThrowException() {
    when(auditLogRepository.findTopByOrderBySeqIdDesc()).thenReturn(Optional.empty());

    assertThatThrownBy(() -> auditService.save(new AuditLogPayload()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("genesis record is missing");
  }

  @Test
  @SuppressWarnings("unchecked")
  void listAuditLogs_ShouldReturnProjectedPage() {
    // Given
    AuditLogSearchCriteria criteria = AuditLogSearchCriteria.builder().build();
    Pageable pageable = PageRequest.of(0, 10);
    AuditLogInfo mockInfo = mock(AuditLogInfo.class);
    Page<AuditLogInfo> page = new PageImpl<>(List.of(mockInfo));

    when(auditLogRepository.findBy(any(Specification.class), any(Function.class))).thenReturn(page);

    // When
    Page<AuditLogInfo> result = auditService.listAuditLogs(criteria, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(auditLogRepository).findBy(any(Specification.class), any(Function.class));
  }

  @Test
  void getAuditLogById_ShouldReturnModel_WhenExists() {
    // Given
    Long seqId = 123L;
    AuditLogEntity entity =
        AuditLogEntity.builder()
            .seqId(seqId)
            .action(AuditAction.SECRET_READ.name())
            .correlationId(UUID.randomUUID())
            .actorUserId(UUID.randomUUID())
            .prevHash(new byte[32])
            .dataHash(new byte[32])
            .build();

    when(auditLogRepository.findById(seqId)).thenReturn(Optional.of(entity));

    // When
    AuditLog result = auditService.getAuditLogById(seqId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getSeqId()).isEqualTo(seqId);
  }

  @Test
  void getAuditLogById_ShouldThrowNotFound_WhenMissing() {
    // Given
    Long seqId = 123L;
    when(auditLogRepository.findById(seqId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> auditService.getAuditLogById(seqId))
        .isInstanceOf(EntityNotFoundException.class);
  }
}
