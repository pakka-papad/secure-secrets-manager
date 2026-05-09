package com.example.secrets_manager.tasks.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.security.SecurityUtils;
import com.example.secrets_manager.security.WithMockAppUser;
import com.example.secrets_manager.tasks.data.converters.TaskEntityConverter;
import com.example.secrets_manager.tasks.data.entities.TaskEntity;
import com.example.secrets_manager.tasks.data.repositories.TaskRepository;
import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tasks.models.TaskType;
import com.example.secrets_manager.tracing.CorrelationContext;
import com.example.secrets_manager.tracing.MissingCorrelationContextException;
import com.example.secrets_manager.tracing.WithCorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock private TaskRepository taskRepository;

  private TaskService taskService;
  private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    TaskEntityConverter taskConverter = new TaskEntityConverter(objectMapper);
    taskService = new TaskService(taskRepository, taskConverter);
  }

  @Test
  @WithCorrelationId
  @WithMockAppUser(TEST_USER_ID)
  void createTask_WithSpecificUser_ShouldSucceed() {
    // Given
    UUID correlationId = CorrelationContext.get().orElseThrow();
    UUID userId = UUID.fromString(TEST_USER_ID);

    when(taskRepository.save(any()))
        .thenAnswer(
            inv -> {
              TaskEntity entity = inv.getArgument(0);
              if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
              }
              return entity;
            });

    // When
    Task result = taskService.createTask(TaskType.MASTER_KEY_MIGRATION, null);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getCorrelationId()).isEqualTo(correlationId);
    assertThat(result.getInitiatorUserId()).isEqualTo(userId);

    verify(taskRepository).save(argThat(entity -> entity.getInitiatorUserId().equals(userId)));
  }

  @Test
  @WithCorrelationId
  @WithMockAppUser(roles = "ADMIN")
  void createTask_WithRandomAdmin_ShouldSucceed() {
    // Given
    UUID correlationId = CorrelationContext.get().orElseThrow();

    // SecurityUtils helper to verify our custom annotation worked
    UUID userId = SecurityUtils.getAuthenticatedUserId();
    assertThat(userId).isNotNull();

    when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // When
    Task result = taskService.createTask(TaskType.MASTER_KEY_MIGRATION, null);

    // Then
    assertThat(result.getInitiatorUserId()).isEqualTo(userId);
    assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_ADMIN");
  }

  @Test
  @WithMockAppUser(TEST_USER_ID)
  void createTask_WithoutTracingContext_ShouldThrowException() {
    CorrelationContext.clear();
    assertThatThrownBy(() -> taskService.createTask(TaskType.MASTER_KEY_MIGRATION, null))
        .isInstanceOf(MissingCorrelationContextException.class);
  }

  @Test
  @WithCorrelationId
  void createTask_WithoutSecurityContext_ShouldThrowException() {
    SecurityContextHolder.clearContext();
    assertThatThrownBy(() -> taskService.createTask(TaskType.MASTER_KEY_MIGRATION, null))
        .isInstanceOf(NullPointerException.class);
  }
}
