package com.example.secrets_manager.tasks.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.repositories.SecretRepository;
import com.example.secrets_manager.core.services.InternalSecretService;
import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.TaskContext;
import com.example.secrets_manager.tasks.models.TaskType;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationExtraInfo;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationInput;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationOutput;
import com.example.secrets_manager.tasks.services.TaskAssignmentService;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class MasterKeyMigrationTaskHandlerTest {

  @Mock private TaskRepository taskRepository;
  @Mock private TaskAssignmentService assignmentService;
  @Mock private TaskEntityConverter taskConverter;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Mock private SecretRepository secretRepository;
  @Mock private InternalSecretService internalSecretService;
  @Mock private Consumer<MasterKeyMigrationExtraInfo> progressUpdater;

  private MasterKeyMigrationTaskHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new MasterKeyMigrationTaskHandler(
            taskRepository,
            assignmentService,
            taskConverter,
            eventPublisher,
            secretRepository,
            internalSecretService);
  }

  @Test
  void getSupportedType_ShouldReturnMasterKeyMigration() {
    assertThat(handler.getSupportedType()).isEqualTo(TaskType.MASTER_KEY_MIGRATION);
  }

  @Test
  void execute_ShouldMigrateSecretsInBatches() throws Exception {
    // Given
    UUID taskId = UUID.randomUUID();
    int targetVersion = 2;
    var input = new MasterKeyMigrationInput(targetVersion);
    var context = new TaskContext<>(taskId, input, progressUpdater);

    UUID secretId1 = UUID.randomUUID();
    UUID secretId2 = UUID.randomUUID();

    // Mock two batches: first has 2 secrets, second is empty
    when(secretRepository.findSecretIdsByMasterKeyVersionLessThan(
            eq(targetVersion), any(Pageable.class)))
        .thenReturn(List.of(secretId1, secretId2))
        .thenReturn(List.of());

    // When
    MasterKeyMigrationOutput result = handler.execute(context);

    // Then
    assertThat(result.getSuccessfullyMigrated()).isEqualTo(2);
    assertThat(result.getTotalFailures()).isZero();
    assertThat(result.getErrorDetails()).isEmpty();

    verify(internalSecretService).upgradeMasterKey(eq(secretId1), eq(targetVersion), eq(taskId));
    verify(internalSecretService).upgradeMasterKey(eq(secretId2), eq(targetVersion), eq(taskId));

    verify(progressUpdater, atLeastOnce()).accept(any());
  }

  @Test
  void execute_ShouldHandlePartialFailures() throws Exception {
    // Given
    UUID taskId = UUID.randomUUID();
    int targetVersion = 2;
    var input = new MasterKeyMigrationInput(targetVersion);
    var context = new TaskContext<>(taskId, input, progressUpdater);

    UUID secretId1 = UUID.randomUUID();
    UUID secretId2 = UUID.randomUUID();

    when(secretRepository.findSecretIdsByMasterKeyVersionLessThan(
            eq(targetVersion), any(Pageable.class)))
        .thenReturn(List.of(secretId1, secretId2))
        .thenReturn(List.of());

    // First succeeds, second fails
    doNothing().when(internalSecretService).upgradeMasterKey(eq(secretId1), anyInt(), any());
    doThrow(new RuntimeException("Crypto error"))
        .when(internalSecretService)
        .upgradeMasterKey(eq(secretId2), anyInt(), any());

    // When
    MasterKeyMigrationOutput result = handler.execute(context);

    // Then
    assertThat(result.getSuccessfullyMigrated()).isEqualTo(1);
    assertThat(result.getTotalFailures()).isEqualTo(1);
    assertThat(result.getErrorDetails()).containsEntry(secretId2, "Crypto error");
  }

  @Test
  void execute_ShouldAbortImmediately_WhenEvictedInLoop() throws Exception {
    // Given
    UUID taskId = UUID.randomUUID();
    int targetVersion = 2;
    var input = new MasterKeyMigrationInput(targetVersion);
    var context = new TaskContext<>(taskId, input, progressUpdater);

    UUID secretId1 = UUID.randomUUID();

    when(secretRepository.findSecretIdsByMasterKeyVersionLessThan(
            eq(targetVersion), any(Pageable.class)))
        .thenReturn(List.of(secretId1));

    // Simulate eviction during the service call
    doThrow(new TaskAssignmentEvictedException(taskId))
        .when(internalSecretService)
        .upgradeMasterKey(any(), anyInt(), any());

    // When & Then
    assertThatThrownBy(() -> handler.execute(context))
        .isInstanceOf(TaskAssignmentEvictedException.class);

    verify(internalSecretService, times(1)).upgradeMasterKey(any(), anyInt(), any());
    verify(progressUpdater, never()).accept(any());
  }
}
