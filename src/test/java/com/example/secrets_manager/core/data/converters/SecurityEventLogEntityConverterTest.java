package com.example.secrets_manager.core.data.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.data.entities.SecurityEventLogEntity;
import com.example.secrets_manager.core.models.SecurityEvent;
import com.example.secrets_manager.core.models.SecurityEventLog;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecurityEventLogEntityConverterTest {

  @Test
  void toModel_ShouldMapAllFieldsCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    UUID correlationId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    Instant now = Instant.now();
    SecurityEventLogEntity entity =
        SecurityEventLogEntity.builder()
            .id(id)
            .correlationId(correlationId)
            .createdAt(now)
            .actorUserId(actorId)
            .action("LOGIN_FAILED")
            .details("{}")
            .build();

    // When
    SecurityEventLog model = SecurityEventLogEntityConverter.toModel(entity);

    // Then
    assertThat(model).isNotNull();
    assertThat(model.getId()).isEqualTo(id);
    assertThat(model.getCorrelationId()).isEqualTo(correlationId);
    assertThat(model.getActorUserId()).isEqualTo(actorId);
    assertThat(model.getAction()).isEqualTo(SecurityEvent.LOGIN_FAILED);
    assertThat(model.getCreatedAt()).isEqualTo(now);
  }

  @Test
  void fromModel_ShouldMapAllFieldsCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    UUID correlationId = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    Instant now = Instant.now();
    SecurityEventLog model =
        SecurityEventLog.builder()
            .id(id)
            .correlationId(correlationId)
            .createdAt(now)
            .actorUserId(actorId)
            .action(SecurityEvent.ACCESS_DENIED)
            .details("{\"reason\":\"test\"}")
            .build();

    // When
    SecurityEventLogEntity entity = SecurityEventLogEntityConverter.fromModel(model);

    // Then
    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getCorrelationId()).isEqualTo(correlationId);
    assertThat(entity.getActorUserId()).isEqualTo(actorId);
    assertThat(entity.getAction()).isEqualTo("ACCESS_DENIED");
    assertThat(entity.getCreatedAt()).isEqualTo(now);
  }
}
