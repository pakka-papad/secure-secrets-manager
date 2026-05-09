package com.example.secrets_manager.tasks.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tasks.models.TaskType;
import com.example.secrets_manager.tracing.CorrelationContext;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class TaskExecutorServiceTest {

  @Mock private TaskHandlerRegistry handlerRegistry;
  @Mock private TaskAssignmentService assignmentService;
  @Mock private ThreadPoolTaskExecutor taskExecutor;
  @Mock private TaskHandler<?, ?> taskHandler;

  private TaskExecutorService executorService;

  @BeforeEach
  void setUp() {
    executorService = new TaskExecutorService(handlerRegistry, assignmentService, taskExecutor);
  }

  @AfterEach
  void tearDown() {
    CorrelationContext.clear();
  }

  @Test
  void submitTask_ShouldPropagateTraceToBackgroundThread() throws Exception {
    // Given
    UUID correlationId = UUID.randomUUID();
    Task task =
        Task.builder()
            .id(UUID.randomUUID())
            .correlationId(correlationId)
            .type(TaskType.MASTER_KEY_MIGRATION)
            .build();

    when(handlerRegistry.getHandler(task.getType()))
        .thenReturn((Optional) Optional.of(taskHandler));
    when(assignmentService.isAssignmentStillValid(task.getId())).thenReturn(true);

    // Track context state during execution
    final var capturedCid = new AtomicReference<>();
    final var capturedMdc = new AtomicReference<>();

    doAnswer(
            inv -> {
              capturedCid.set(CorrelationContext.get().orElse(null));
              capturedMdc.set(MDC.get("correlationId"));
              return null;
            })
        .when(taskHandler)
        .runPreExecute(task);

    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

    // When
    executorService.submitTask(task);

    // Then
    verify(taskExecutor).execute(runnableCaptor.capture());
    Runnable capturedRunnable = runnableCaptor.getValue();

    // Verify background thread context (simulated by running the captured runnable)
    capturedRunnable.run();

    // Verify state DURING execution
    assertThat(capturedCid.get()).isEqualTo(correlationId);
    assertThat(capturedMdc.get()).isEqualTo(correlationId.toString());

    // Verify that after execution, context is still empty (ensures cleanup happened)
    assertThat(CorrelationContext.get()).isEmpty();
    assertThat(MDC.get("correlationId")).isNull();

    // To truly verify that the context was set DURING execution, we would need to mock the handler
    // to check the context.
    verify(taskHandler).runPreExecute(task);
    verify(taskHandler).execute(any());
  }
}
