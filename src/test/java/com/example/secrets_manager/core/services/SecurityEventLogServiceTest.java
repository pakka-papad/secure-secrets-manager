package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.SecurityEventLogEntity;
import com.example.secrets_manager.core.data.repositories.SecurityEventLogInfo;
import com.example.secrets_manager.core.data.repositories.SecurityEventLogRepository;
import com.example.secrets_manager.core.models.SecurityEvent;
import com.example.secrets_manager.core.models.SecurityEventLog;
import com.example.secrets_manager.core.models.SecurityEventLogPayload;
import com.example.secrets_manager.core.models.search.SecurityEventSearchCriteria;
import com.example.secrets_manager.tracing.CorrelationContext;
import com.example.secrets_manager.tracing.MissingCorrelationContextException;
import com.example.secrets_manager.tracing.WithCorrelationId;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SecurityEventLogServiceTest {

  @Mock private SecurityEventLogRepository repository;
  @InjectMocks private SecurityEventLogService service;

  @Test
  @WithCorrelationId
  void save_ShouldPersistEvent() {
    // Given
    UUID correlationId = CorrelationContext.get().orElseThrow();

    UUID actorId = UUID.randomUUID();
    SecurityEventLogPayload payload =
        SecurityEventLogPayload.builder()
            .actorUserId(actorId)
            .action(SecurityEvent.LOGIN_FAILED)
            .details("{\"reason\":\"wrong pass\"}")
            .build();

    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // When
    SecurityEventLog result = service.save(payload);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getCorrelationId()).isEqualTo(correlationId);
    assertThat(result.getAction()).isEqualTo(SecurityEvent.LOGIN_FAILED);
    assertThat(result.getActorUserId()).isEqualTo(actorId);
    verify(repository).save(any(SecurityEventLogEntity.class));
  }

  @Test
  @WithCorrelationId
  void save_WithExplicitCorrelationIdInPayload_ShouldUsePayloadId() {
    // Given
    UUID payloadCid = UUID.randomUUID();

    SecurityEventLogPayload payload =
        SecurityEventLogPayload.builder()
            .actorUserId(UUID.randomUUID())
            .action(SecurityEvent.LOGIN_FAILED)
            .correlationId(payloadCid)
            .build();

    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // When
    SecurityEventLog result = service.save(payload);

    // Then
    assertThat(result.getCorrelationId()).isEqualTo(payloadCid);
  }

  @Test
  void save_WithoutCorrelationId_ShouldThrowMissingCorrelationContextException() {
    // Given
    CorrelationContext.clear();
    SecurityEventLogPayload payload = new SecurityEventLogPayload();

    // When & Then
    assertThatThrownBy(() -> service.save(payload))
        .isInstanceOf(MissingCorrelationContextException.class)
        .hasMessageContaining("without a Correlation ID");
  }

  @Test
  @SuppressWarnings("unchecked")
  void listSecurityEvents_ShouldReturnProjectedPage() {
    // Given
    SecurityEventSearchCriteria criteria = SecurityEventSearchCriteria.builder().build();
    Pageable pageable = PageRequest.of(0, 10);
    SecurityEventLogInfo mockInfo = mock(SecurityEventLogInfo.class);
    Page<SecurityEventLogInfo> page = new PageImpl<>(List.of(mockInfo));

    when(repository.findBy(any(Specification.class), any(Function.class))).thenReturn(page);

    // When
    Page<SecurityEventLogInfo> result = service.listSecurityEvents(criteria, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    verify(repository).findBy(any(Specification.class), any(Function.class));
  }

  @Test
  void getSecurityEventById_ShouldReturnModel_WhenExists() {
    // Given
    UUID id = UUID.randomUUID();
    SecurityEventLogEntity entity =
        SecurityEventLogEntity.builder()
            .id(id)
            .action(SecurityEvent.ACCESS_DENIED.name())
            .createdAt(Instant.now())
            .build();

    when(repository.findById(id)).thenReturn(Optional.of(entity));

    // When
    SecurityEventLog result = service.getSecurityEventById(id);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(id);
  }

  @Test
  void getSecurityEventById_ShouldThrowNotFound_WhenMissing() {
    // Given
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> service.getSecurityEventById(id))
        .isInstanceOf(EntityNotFoundException.class);
  }
}
