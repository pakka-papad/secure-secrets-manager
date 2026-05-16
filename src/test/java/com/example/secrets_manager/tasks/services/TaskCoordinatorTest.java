package com.example.secrets_manager.tasks.services;

import static org.mockito.Mockito.*;

import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.entities.TaskEntity;
import com.example.secrets_manager.tasks.data.repositories.TaskAssignmentRepository;
import com.example.secrets_manager.tasks.data.repositories.TaskCandidate;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaskCoordinatorTest {

  @Mock private TaskRepository taskRepository;
  @Mock private TaskAssignmentRepository assignmentRepository;
  @Mock private TaskAssignmentService assignmentService;
  @Mock private TaskExecutorService executorService;
  @Mock private TaskHandlerRegistry handlerRegistry;

  private TaskCoordinator coordinator;

  @BeforeEach
  void setUp() {
    final var objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    final var taskConverter = new TaskEntityConverter(objectMapper);

    coordinator =
        new TaskCoordinator(
            taskRepository,
            assignmentRepository,
            assignmentService,
            executorService,
            taskConverter,
            handlerRegistry);
    ReflectionTestUtils.setField(coordinator, "batchSize", 50);
    ReflectionTestUtils.setField(coordinator, "candidateLimit", 200);
    ReflectionTestUtils.setField(coordinator, "stalenessThreshold", Duration.ofSeconds(60));
  }

  @Test
  void pollPendingTasks_ShouldClaimAndSubmitTasks_WhenSupported() {
    // Given
    UUID taskId = UUID.randomUUID();
    String type = TaskType.MASTER_KEY_MIGRATION.name();
    TaskCandidate candidate = mock(TaskCandidate.class);
    when(candidate.getId()).thenReturn(taskId);
    when(candidate.getType()).thenReturn(type);

    when(taskRepository.findPendingCandidates(TaskState.PENDING.name(), 200))
        .thenReturn(List.of(candidate));
    when(handlerRegistry.isSupported(type)).thenReturn(true);
    when(assignmentService.claimTask(taskId)).thenReturn(true);

    TaskEntity entity = new TaskEntity();
    entity.setId(taskId);
    entity.setType(type);
    entity.setState(TaskState.PENDING.name());
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(entity));

    // When
    coordinator.pollPendingTasks();

    // Then
    verify(executorService).submitTask(argThat(task -> task.getId().equals(taskId)));
  }

  @Test
  void pollPendingTasks_ShouldSkip_WhenNotSupported() {
    // Given
    UUID taskId = UUID.randomUUID();
    String type = "UNKNOWN_TYPE";
    TaskCandidate candidate = mock(TaskCandidate.class);
    when(candidate.getType()).thenReturn(type);

    when(taskRepository.findPendingCandidates(TaskState.PENDING.name(), 200))
        .thenReturn(List.of(candidate));
    when(handlerRegistry.isSupported(type)).thenReturn(false);

    // When
    coordinator.pollPendingTasks();

    // Then
    verify(assignmentService, never()).claimTask(any());
    verify(executorService, never()).submitTask(any());
  }

  @Test
  void pollPendingTasks_ShouldNotSubmit_IfClaimFails() {
    // Given
    UUID taskId = UUID.randomUUID();
    String type = TaskType.MASTER_KEY_MIGRATION.name();
    TaskCandidate candidate = mock(TaskCandidate.class);
    when(candidate.getId()).thenReturn(taskId);
    when(candidate.getType()).thenReturn(type);

    when(taskRepository.findPendingCandidates(TaskState.PENDING.name(), 200))
        .thenReturn(List.of(candidate));
    when(handlerRegistry.isSupported(type)).thenReturn(true);
    when(assignmentService.claimTask(taskId)).thenReturn(false);

    // When
    coordinator.pollPendingTasks();

    // Then
    verify(executorService, never()).submitTask(any());
  }

  @Test
  void pollStaleTasks_ShouldReclaimAndSubmitTasks_WhenSupported() {
    // Given
    UUID taskId = UUID.randomUUID();
    String type = TaskType.MASTER_KEY_MIGRATION.name();
    TaskCandidate candidate = mock(TaskCandidate.class);
    when(candidate.getId()).thenReturn(taskId);
    when(candidate.getType()).thenReturn(type);

    when(assignmentRepository.findStaleCandidates(any(Duration.class), eq(200)))
        .thenReturn(List.of(candidate));
    when(handlerRegistry.isSupported(type)).thenReturn(true);
    when(assignmentService.reclaimTask(taskId)).thenReturn(true);

    TaskEntity entity = new TaskEntity();
    entity.setId(taskId);
    entity.setType(type);
    entity.setState(TaskState.PENDING.name());
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(entity));

    // When
    coordinator.pollStaleTasks();

    // Then
    verify(executorService).submitTask(argThat(task -> task.getId().equals(taskId)));
  }
}
