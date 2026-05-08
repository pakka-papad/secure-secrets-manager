package com.example.secrets_manager.core.data.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.data.entities.AuditLogEntity;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLog;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditLogEntityConverterTest {

  @Test
  void toModel_ShouldMapAllFields() {
    UUID correlationId = UUID.randomUUID();
    AuditLogEntity entity =
        AuditLogEntity.builder()
            .seqId(1L)
            .correlationId(correlationId)
            .action(AuditAction.SECRET_CREATE.name())
            .actorUserId(UUID.randomUUID())
            .createdAt(Instant.now())
            .prevHash(new byte[] {1})
            .dataHash(new byte[] {2})
            .build();

    AuditLog model = AuditLogEntityConverter.toModel(entity);

    assertThat(model.getCorrelationId()).isEqualTo(correlationId);
    assertThat(model.getSeqId()).isEqualTo(1L);
    assertThat(model.getAction()).isEqualTo(AuditAction.SECRET_CREATE);
  }

  @Test
  void fromModel_ShouldMapAllFields() {
    UUID correlationId = UUID.randomUUID();
    AuditLog model =
        AuditLog.builder()
            .seqId(1L)
            .correlationId(correlationId)
            .action(AuditAction.SECRET_CREATE)
            .actorUserId(UUID.randomUUID())
            .createdAt(Instant.now())
            .prevHash(new byte[] {1})
            .dataHash(new byte[] {2})
            .build();

    AuditLogEntity entity = AuditLogEntityConverter.fromModel(model);

    assertThat(entity.getCorrelationId()).isEqualTo(correlationId);
    assertThat(entity.getSeqId()).isEqualTo(1L);
    assertThat(entity.getAction()).isEqualTo(AuditAction.SECRET_CREATE.name());
  }
}
