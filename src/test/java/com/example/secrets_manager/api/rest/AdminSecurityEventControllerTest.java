package com.example.secrets_manager.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.api.rest.advice.AppExceptionHandler;
import com.example.secrets_manager.core.data.repositories.SecurityEventLogInfo;
import com.example.secrets_manager.core.models.SecurityEvent;
import com.example.secrets_manager.core.models.SecurityEventLog;
import com.example.secrets_manager.core.services.SecurityEventLogService;
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
class AdminSecurityEventControllerTest {

  private MockMvc mockMvc;

  @Mock private SecurityEventLogService securityEventLogService;
  @InjectMocks private AdminSecurityEventController controller;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new AppExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  @Test
  void listSecurityEvents_ShouldReturnPagedResults() throws Exception {
    // Given
    SecurityEventLogInfo info = Mockito.mock(SecurityEventLogInfo.class);
    when(info.getId()).thenReturn(UUID.randomUUID());
    when(info.getAction()).thenReturn(SecurityEvent.LOGIN_FAILED.name());
    when(info.getCreatedAt()).thenReturn(Instant.now());

    when(securityEventLogService.listSecurityEvents(any(), any()))
        .thenReturn(new PageImpl<>(List.of(info)));

    // When & Then
    mockMvc
        .perform(get("/api/v1/admin/security-events").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].action").value("LOGIN_FAILED"));
  }

  @Test
  void getSecurityEventById_ShouldReturnDetailedResult() throws Exception {
    // Given
    UUID eventId = UUID.randomUUID();
    SecurityEventLog model =
        SecurityEventLog.builder()
            .id(eventId)
            .action(SecurityEvent.ACCESS_DENIED)
            .createdAt(Instant.now())
            .details("{}")
            .build();

    when(securityEventLogService.getSecurityEventById(eventId)).thenReturn(model);

    // When & Then
    mockMvc
        .perform(get("/api/v1/admin/security-events/" + eventId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(eventId.toString()))
        .andExpect(jsonPath("$.action").value("ACCESS_DENIED"));
  }
}
