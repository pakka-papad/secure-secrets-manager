package com.example.secrets_manager.api.rest.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.secrets_manager.api.rest.dto.TaskDetailedResponse;
import com.example.secrets_manager.api.rest.dto.TaskSummaryResponse;
import com.example.secrets_manager.tasks.data.repositories.TaskInfo;
import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskResponseConverterTest {

  @Test
  void toSummaryResponse_FromDomain_ShouldMapFields() {
    // Given
    Task model =
        Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.MASTER_KEY_MIGRATION)
            .state(TaskState.RUNNING)
            .createdAt(Instant.now())
            .initiatorUserId(UUID.randomUUID())
            .correlationId(UUID.randomUUID())
            .build();

    // When
    TaskSummaryResponse response = TaskResponseConverter.toSummaryResponse(model);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(model.getId());
    assertThat(response.getType()).isEqualTo(model.getType());
    assertThat(response.getState()).isEqualTo(model.getState());
  }

  @Test
  void toSummaryResponse_FromProjection_ShouldMapFields() {
    // Given
    TaskInfo info = mock(TaskInfo.class);
    UUID id = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    UUID cid = UUID.randomUUID();
    Instant now = Instant.now();

    when(info.getId()).thenReturn(id);
    when(info.getType()).thenReturn(TaskType.MASTER_KEY_MIGRATION.name());
    when(info.getState()).thenReturn(TaskState.RUNNING.name());
    when(info.getCreatedAt()).thenReturn(now);
    when(info.getInitiatorUserId()).thenReturn(actorId);
    when(info.getCorrelationId()).thenReturn(cid);

    // When
    TaskSummaryResponse response = TaskResponseConverter.toSummaryResponse(info);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(id);
    assertThat(response.getType()).isEqualTo(TaskType.MASTER_KEY_MIGRATION);
    assertThat(response.getState()).isEqualTo(TaskState.RUNNING);
    assertThat(response.getCreatedAt()).isEqualTo(now);
    assertThat(response.getInitiatorUserId()).isEqualTo(actorId);
    assertThat(response.getCorrelationId()).isEqualTo(cid);
  }

  @Test
  void toDetailedResponse_ShouldMapAllFields() {
    // Given
    Task model =
        Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.MASTER_KEY_MIGRATION)
            .state(TaskState.COMPLETED)
            .createdAt(Instant.now())
            .startedAt(Instant.now().plusSeconds(1))
            .completedAt(Instant.now().plusSeconds(10))
            .initiatorUserId(UUID.randomUUID())
            .correlationId(UUID.randomUUID())
            .metadata("{\"test\": true}")
            .build();

    // When
    TaskDetailedResponse response = TaskResponseConverter.toDetailedResponse(model);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(model.getId());
    assertThat(response.getMetadata()).isEqualTo(model.getMetadata());
    assertThat(response.getCompletedAt()).isEqualTo(model.getCompletedAt());
  }

  @Test
  void toSummaryResponse_ShouldHandleNull() {
    assertThat(TaskResponseConverter.toSummaryResponse((Task) null)).isNull();
    assertThat(TaskResponseConverter.toSummaryResponse((TaskInfo) null)).isNull();
  }
}
