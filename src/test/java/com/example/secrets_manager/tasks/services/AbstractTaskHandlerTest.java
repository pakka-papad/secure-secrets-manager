package com.example.secrets_manager.tasks.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.*;
import com.example.secrets_manager.tasks.models.events.TaskStartedEvent;
import com.example.secrets_manager.tasks.models.events.TaskStoppedEvent;
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

  private TestHandler handler;

  @BeforeEach
  void setUp() {
    final var objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    final var taskConverter = new TaskEntityConverter(objectMapper);
    handler = new TestHandler(taskRepository, assignmentService, taskConverter, eventPublisher);
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

    verify(eventPublisher).publishEvent(any(TaskStartedEvent.class));
    verify(eventPublisher).publishEvent(any(TaskStoppedEvent.class));
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
    when(taskRepository.updateFenced(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    // When
    handler.run(task);

    // Then
    assertThat(handler.isExecuteCalled()).isFalse();
    // Cleanup always runs in finally block, releasing the task
    verify(assignmentService).releaseTask(task.getId());
  }

  @Test
  void persistStateWithFencing_ShouldThrowEvictedException_WhenUpdateReturnsZero() {
    // Given
    Task task =
        Task.builder()
            .id(UUID.randomUUID())
            .type(TaskType.MASTER_KEY_MIGRATION)
            .state(TaskState.PENDING)
            .correlationId(UUID.randomUUID())
            .initiatorUserId(UUID.randomUUID())
            .build();
    when(taskRepository.updateFenced(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(0);

    // When & Then
    assertThatThrownBy(() -> handler.persistStateWithFencing(task))
        .isInstanceOf(TaskAssignmentEvictedException.class);

    verify(eventPublisher).publishEvent(any(TaskStoppedEvent.class));
  }

  @Test
  void abort_ShouldPublishEventAndThrowCorrectException() {
    // Given
    Task task = Task.builder().id(UUID.randomUUID()).build();

    // When & Then
    assertThatThrownBy(
            () -> handler.callAbort(AbstractTaskHandler.AbortReason.EVICTED, task.getId()))
        .isInstanceOf(TaskAssignmentEvictedException.class);

    verify(eventPublisher).publishEvent(any(TaskStoppedEvent.class));
  }

  // --- Test Support Classes ---

  private static class TestInput implements TaskInput {}

  private static class TestOutput implements TaskOutput {
    private final String val;

    TestOutput(String val) {
      this.val = val;
    }
  }

  private static class TestHandler extends AbstractTaskHandler<TestInput, TestOutput> {
    private TestOutput executeResult;
    private Exception executeException;
    private boolean executeCalled = false;

    public TestHandler(
        TaskRepository tr,
        TaskAssignmentService as,
        TaskEntityConverter tc,
        ApplicationEventPublisher ep) {
      super(tr, as, tc, ep);
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
    protected TestOutput execute(TaskContext<TestInput> context) throws Exception {
      this.executeCalled = true;
      if (executeException != null) throw executeException;
      return executeResult;
    }

    void callAbort(AbortReason reason, UUID taskId) {
      this.abort(reason, taskId);
    }
  }
}
