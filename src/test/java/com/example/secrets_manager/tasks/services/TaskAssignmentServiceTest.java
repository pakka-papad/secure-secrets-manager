package com.example.secrets_manager.tasks.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.tasks.data.entities.TaskAssignmentEntity;
import com.example.secrets_manager.tasks.data.repositories.TaskAssignmentRepository;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskAssignmentServiceTest {

  @Mock private TaskAssignmentRepository assignmentRepository;
  @Mock private WorkerService workerService;

  private TaskAssignmentService assignmentService;

  @BeforeEach
  void setUp() {
    assignmentService = new TaskAssignmentService(assignmentRepository, workerService);
  }

  @Test
  void claimTask_ShouldReturnTrue_WhenRepositoryUpdatesOneRow() {
    // Given
    UUID taskId = UUID.randomUUID();
    when(assignmentRepository.atomicClaim(taskId, TaskUtils.WORKER_ID)).thenReturn(1);

    // When
    boolean result = assignmentService.claimTask(taskId);

    // Then
    assertThat(result).isTrue();
    verify(workerService).registerWorker();
    verify(assignmentRepository).atomicClaim(taskId, TaskUtils.WORKER_ID);
  }

  @Test
  void claimTask_ShouldReturnFalse_WhenRepositoryUpdatesZeroRows() {
    // Given
    UUID taskId = UUID.randomUUID();
    when(assignmentRepository.atomicClaim(taskId, TaskUtils.WORKER_ID)).thenReturn(0);

    // When
    boolean result = assignmentService.claimTask(taskId);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void claimTask_SimultaneousRace_OnlyOneShouldSucceed() {
    // Given
    UUID taskId = UUID.randomUUID();

    // First attempt succeeds
    when(assignmentRepository.atomicClaim(taskId, TaskUtils.WORKER_ID)).thenReturn(1);
    boolean firstResult = assignmentService.claimTask(taskId);

    // Second attempt fails because row already exists/updated
    when(assignmentRepository.atomicClaim(taskId, TaskUtils.WORKER_ID)).thenReturn(0);
    boolean secondResult = assignmentService.claimTask(taskId);

    // Then
    assertThat(firstResult).isTrue();
    assertThat(secondResult).isFalse();
    verify(assignmentRepository, times(2)).atomicClaim(eq(taskId), any());
  }

  @Test
  void reclaimTask_ShouldDeleteAndThenClaim() {
    // Given
    UUID taskId = UUID.randomUUID();
    when(assignmentRepository.atomicClaim(taskId, TaskUtils.WORKER_ID)).thenReturn(1);

    // When
    boolean result = assignmentService.reclaimTask(taskId);

    // Then
    assertThat(result).isTrue();
    verify(assignmentRepository).deleteById(taskId);
    verify(assignmentRepository).atomicClaim(taskId, TaskUtils.WORKER_ID);
  }

  @Test
  void isAssignmentStillValid_ShouldReturnTrue_WhenWorkerIdMatches() {
    // Given
    UUID taskId = UUID.randomUUID();
    var entity = new TaskAssignmentEntity();
    entity.setTaskId(taskId);
    entity.setWorkerId(TaskUtils.WORKER_ID);

    when(assignmentRepository.findById(taskId)).thenReturn(Optional.of(entity));

    // When
    boolean result = assignmentService.isAssignmentStillValid(taskId);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isAssignmentStillValid_ShouldReturnFalse_WhenWorkerIdMismatches() {
    // Given
    UUID taskId = UUID.randomUUID();
    var entity = new TaskAssignmentEntity();
    entity.setTaskId(taskId);
    entity.setWorkerId(UUID.randomUUID()); // Different worker

    when(assignmentRepository.findById(taskId)).thenReturn(Optional.of(entity));

    // When
    boolean result = assignmentService.isAssignmentStillValid(taskId);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void releaseTask_ShouldDeleteAssignment() {
    // Given
    UUID taskId = UUID.randomUUID();

    // When
    assignmentService.releaseTask(taskId);

    // Then
    verify(assignmentRepository).deleteById(taskId);
  }
}
