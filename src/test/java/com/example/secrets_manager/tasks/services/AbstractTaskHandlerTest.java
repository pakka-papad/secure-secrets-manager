package com.example.secrets_manager.tasks.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.*;
import com.example.secrets_manager.tasks.services.exceptions.TaskAssignmentEvictedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AbstractTaskHandlerTest {

  @Mock private TaskRepository taskRepository;
  @Mock private TaskAssignmentService assignmentService;
  @Mock private ApplicationEventPublisher eventPublisher;

  private TaskExecutionOrchestrator orchestrator;
  private TestHandler handler;

  @BeforeEach
  void setUp() {
    final var objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    final var taskConverter = new TaskEntityConverter(objectMapper);

    // We use a real orchestrator but spy on it to verify delegation
    orchestrator =
        spy(
            new TaskExecutionOrchestrator(
                taskRepository, assignmentService, taskConverter, eventPublisher));

    handler = new TestHandler(orchestrator, assignmentService, eventPublisher);
  }

  @Test
  void run_ShouldExecuteFullLifecycle_OnSuccess() throws Exception {
    // Given
    Task task =
        Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.MASTER_KEY_MIGRATION)
            .state(TaskState.PENDING)
            .correlationId(UUID.randomUUID())
            .initiatorUserId(UUID.randomUUID())
            .build();
    var output = new TestOutput("done");
    handler.setExecuteResult(output);

    when(assignmentService.isAssignmentStillValid(task.getId())).thenReturn(true);
    when(taskRepository.updateFenced(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    // When
    handler.run(task);

    // Then
    assertThat(task.getState()).isEqualTo(TaskState.COMPLETED);
    assertThat(task.getOutput()).isEqualTo(output);

    verify(orchestrator).startTask(eq(task), any());
    verify(orchestrator).completeTask(eq(task), eq(output), any());
    verify(assignmentService).releaseTask(task.getId());
  }

  @Test
  void run_ShouldMarkFailed_OnException() throws Exception {
    // Given
    Task task =
        Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.MASTER_KEY_MIGRATION)
            .state(TaskState.PENDING)
            .correlationId(UUID.randomUUID())
            .initiatorUserId(UUID.randomUUID())
            .build();
    handler.setExecuteException(new RuntimeException("Business error"));

    when(assignmentService.isAssignmentStillValid(task.getId())).thenReturn(true);
    when(taskRepository.updateFenced(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    // When
    handler.run(task);

    // Then
    assertThat(task.getState()).isEqualTo(TaskState.FAILED);
    verify(orchestrator).failTask(eq(task), any(), any());
    verify(assignmentService).releaseTask(task.getId());
  }

  @Test
  void run_ShouldAbort_WhenAssignmentLostBeforeExecute() throws Exception {
    // Given
    Task task =
        Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.MASTER_KEY_MIGRATION)
            .state(TaskState.PENDING)
            .correlationId(UUID.randomUUID())
            .initiatorUserId(UUID.randomUUID())
            .build();
    when(assignmentService.isAssignmentStillValid(task.getId())).thenReturn(false);
    // Use doAnswer for startTask because it executes the preExecuteHook
    doAnswer(
            invocation -> {
              Runnable hook = invocation.getArgument(1);
              hook.run();
              return null;
            })
        .when(orchestrator)
        .startTask(any(), any());

    // When
    handler.run(task);

    // Then
    assertThat(handler.isExecuteCalled()).isFalse();
    // Cleanup always runs in finally block, releasing the task
    verify(assignmentService).releaseTask(task.getId());
  }

  @Test
  void abort_ShouldThrowCorrectException() {
    // Given
    Task task = Task.builder().id(UUID.randomUUID()).build();

    // When & Then
    assertThatThrownBy(
            () -> handler.callAbort(AbstractTaskHandler.AbortReason.EVICTED, task.getId()))
        .isInstanceOf(TaskAssignmentEvictedException.class);
  }

  // --- Test Support Classes ---

  private static class TestInput implements TaskInput {}

  private static class TestOutput implements TaskOutput {
    private final String val;

    TestOutput(String val) {
      this.val = val;
    }
  }

  private static class TestExtraInfo implements TaskStateExtraInfo {}

  private static class TestHandler
      extends AbstractTaskHandler<TestInput, TestOutput, TestExtraInfo> {
    private TestOutput executeResult;
    private Exception executeException;
    private boolean executeCalled = false;

    public TestHandler(
        TaskExecutionOrchestrator orchestrator,
        TaskAssignmentService as,
        ApplicationEventPublisher ep) {
      super(orchestrator, as, ep);
    }

    void setExecuteResult(TestOutput res) {
      this.executeResult = res;
    }

    void setExecuteException(Exception ex) {
      this.executeException = ex;
    }

    boolean isExecuteCalled() {
      return executeCalled;
    }

    @Override
    public TaskType getSupportedType() {
      return null;
    }

    @Override
    protected TestOutput execute(TaskContext<TestInput, TestExtraInfo> context) throws Exception {
      this.executeCalled = true;
      if (executeException != null) throw executeException;
      return executeResult;
    }

    void callAbort(AbortReason reason, UUID taskId) {
      this.abort(reason, taskId);
    }
  }
}
