package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.secrets_manager.core.data.entities.SecurityEventLogEntity;
import com.example.secrets_manager.core.data.repositories.SecurityEventLogRepository;
import com.example.secrets_manager.core.models.SecurityEvent;
import com.example.secrets_manager.core.models.SecurityEventLog;
import com.example.secrets_manager.core.models.SecurityEventLogPayload;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityEventLogServiceTest {

  @Mock private SecurityEventLogRepository repository;
  @InjectMocks private SecurityEventLogService service;

  @Test
  void save_ShouldPersistEvent() {
    // Given
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
    assertThat(result.getAction()).isEqualTo(SecurityEvent.LOGIN_FAILED);
    assertThat(result.getActorUserId()).isEqualTo(actorId);
    verify(repository).save(any(SecurityEventLogEntity.class));
  }
}
