package com.example.secrets_manager.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.api.rest.advice.AppExceptionHandler;
import com.example.secrets_manager.tasks.data.repositories.TaskInfo;
import com.example.secrets_manager.tasks.models.Task;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import com.example.secrets_manager.tasks.services.TaskService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminTaskControllerTest {

  private MockMvc mockMvc;

  @Mock private TaskService taskService;
  @InjectMocks private AdminTaskController controller;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new AppExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  @Test
  void listTasks_ShouldReturnPagedResults() throws Exception {
    // Given
    TaskInfo info = Mockito.mock(TaskInfo.class);
    when(info.getId()).thenReturn(UUID.randomUUID());
    when(info.getType()).thenReturn(TaskType.MASTER_KEY_MIGRATION.name());
    when(info.getState()).thenReturn(TaskState.PENDING.name());
    when(info.getCreatedAt()).thenReturn(Instant.now());

    when(taskService.listTasks(any(), any())).thenReturn(new PageImpl<>(List.of(info)));

    // When & Then
    mockMvc
        .perform(get("/api/v1/admin/tasks").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].type").value("MASTER_KEY_MIGRATION"));
  }

  @Test
  void getTaskById_ShouldReturnDetailedResult() throws Exception {
    // Given
    UUID taskId = UUID.randomUUID();
    Task model =
        Task.builder()
            .id(taskId)
            .type(TaskType.MASTER_KEY_MIGRATION)
            .state(TaskState.RUNNING)
            .createdAt(Instant.now())
            .initiatorUserId(UUID.randomUUID())
            .correlationId(UUID.randomUUID())
            .build();

    when(taskService.getTaskById(taskId)).thenReturn(model);

    // When & Then
    mockMvc
        .perform(get("/api/v1/admin/tasks/" + taskId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(taskId.toString()))
        .andExpect(jsonPath("$.state").value("RUNNING"));
  }
}
