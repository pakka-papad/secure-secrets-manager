package com.example.secrets_manager.tasks.data.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.tasks.data.entities.TaskEntity;
import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationInput;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskEntityConverterTest {

  private TaskEntityConverter converter;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    converter = new TaskEntityConverter(objectMapper);
  }

  @Test
  void bidirectional_ShouldMaintainIntegrity_WithPolymorphicPayloads() throws Exception {
    // Given
    UUID taskId = UUID.randomUUID();
    UUID correlationId = UUID.randomUUID();
    var input = new MasterKeyMigrationInput(5);
    var output = new MasterKeyMigrationOutput(10, 0, Map.of());

    Task model =
        Task.builder()
            .id(taskId)
            .correlationId(correlationId)
            .type(TaskType.MASTER_KEY_MIGRATION)
            .state(TaskState.COMPLETED)
            .input(input)
            .output(output)
            .createdAt(Instant.now())
            .build();

    // When
    TaskEntity entity = converter.fromModel(model);
    Task backToModel = converter.toModel(entity);

    // Then
    assertThat(backToModel.getId()).isEqualTo(taskId);
    assertThat(backToModel.getCorrelationId()).isEqualTo(correlationId);
    assertThat(backToModel.getType()).isEqualTo(TaskType.MASTER_KEY_MIGRATION);

    // Verify Input (Polymorphic)
    assertThat(backToModel.getInput()).isInstanceOf(MasterKeyMigrationInput.class);
    assertThat(((MasterKeyMigrationInput) backToModel.getInput()).getTargetMasterKeyVersion())
        .isEqualTo(5);

    // Verify Output (Polymorphic)
    assertThat(backToModel.getOutput()).isInstanceOf(MasterKeyMigrationOutput.class);
    assertThat(((MasterKeyMigrationOutput) backToModel.getOutput()).getSuccessfullyMigrated())
        .isEqualTo(10);
  }

  @Test
  void toModel_ShouldHandleNullsGracefully() {
    assertThat(converter.toModel(null)).isNull();

    TaskEntity entity = new TaskEntity();
    entity.setType(TaskType.MASTER_KEY_MIGRATION.name());
    entity.setState(TaskState.PENDING.name());

    Task model = converter.toModel(entity);
    assertThat(model).isNotNull();
    assertThat(model.getInput()).isNull();
  }
}
