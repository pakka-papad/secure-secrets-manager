package com.example.secrets_manager.tasks.services;

import static org.mockito.Mockito.*;

import com.example.secrets_manager.tasks.data.repositories.WorkerRegistryRepository;
import com.example.secrets_manager.tasks.utils.TaskUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkerServiceTest {

  @Mock private WorkerRegistryRepository workerRepository;
  @Mock private LocalTaskRegistry localTaskRegistry;

  private WorkerService service;
  private final Clock fixedClock =
      Clock.fixed(Instant.parse("2026-05-09T10:00:00Z"), ZoneId.systemDefault());

  @BeforeEach
  void setUp() {
    service = new WorkerService(workerRepository, localTaskRegistry, fixedClock);
    ReflectionTestUtils.setField(service, "heartbeatThrottleMs", 5000);
  }

  @Test
  void registerWorker_ShouldUpsertWhenFirstCalled() {
    // When
    service.registerWorker();

    // Then
    verify(workerRepository).upsertHeartbeat(TaskUtils.WORKER_ID);
  }

  @Test
  void registerWorker_ShouldThrottleSubsequentCalls() {
    // Given
    service.registerWorker(); // First call sets lastDbHeartbeat (immediate update because no
    // transaction)
    reset(workerRepository);

    // When (Call again immediately)
    service.registerWorker();

    // Then
    verify(workerRepository, never()).upsertHeartbeat(any());
  }

  @Test
  void sendHeartbeat_ShouldUpsert_IfTasksActiveAndNotThrottled() {
    // Given
    when(localTaskRegistry.hasActiveTasks()).thenReturn(true);

    // When
    service.sendHeartbeat();

    // Then
    verify(workerRepository).upsertHeartbeat(TaskUtils.WORKER_ID);
  }

  @Test
  void sendHeartbeat_ShouldNotUpsert_IfNoActiveTasks() {
    // Given
    when(localTaskRegistry.hasActiveTasks()).thenReturn(false);

    // When
    service.sendHeartbeat();

    // Then
    verify(workerRepository, never()).upsertHeartbeat(any());
  }

  @Test
  void sendHeartbeat_ShouldReactImmediatelyToTaskStopped() {
    // Given: Tasks were active, but now they are gone
    when(localTaskRegistry.hasActiveTasks()).thenReturn(false);

    // When
    service.sendHeartbeat();

    // Then: No DB update should happen
    verify(workerRepository, never()).upsertHeartbeat(any());
  }

  @Test
  void cleanup_ShouldDeleteWorkerRow() {
    // When
    service.cleanup();

    // Then
    verify(workerRepository).deleteById(TaskUtils.WORKER_ID);
  }
}
