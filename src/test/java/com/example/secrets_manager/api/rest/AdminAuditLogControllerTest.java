package com.example.secrets_manager.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.api.rest.advice.AppExceptionHandler;
import com.example.secrets_manager.core.data.repositories.AuditLogInfo;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLog;
import com.example.secrets_manager.core.services.AuditService;
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
class AdminAuditLogControllerTest {

  private MockMvc mockMvc;

  @Mock private AuditService auditService;
  @InjectMocks private AdminAuditLogController controller;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new AppExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  @Test
  void listAuditLogs_ShouldReturnPagedResults() throws Exception {
    // Given
    AuditLogInfo info = Mockito.mock(AuditLogInfo.class);
    when(info.getSeqId()).thenReturn(1L);
    when(info.getAction()).thenReturn(AuditAction.SECRET_CREATE.name());
    when(info.getCreatedAt()).thenReturn(Instant.now());

    when(auditService.listAuditLogs(any(), any())).thenReturn(new PageImpl<>(List.of(info)));

    // When & Then
    mockMvc
        .perform(get("/api/v1/admin/audit-logs").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].action").value("SECRET_CREATE"));
  }

  @Test
  void getAuditLogById_ShouldReturnDetailedResult() throws Exception {
    // Given
    Long seqId = 123L;
    AuditLog model =
        AuditLog.builder()
            .seqId(seqId)
            .action(AuditAction.SECRET_READ)
            .correlationId(UUID.randomUUID())
            .createdAt(Instant.now())
            .prevHash(new byte[32])
            .dataHash(new byte[32])
            .build();

    when(auditService.getAuditLogById(seqId)).thenReturn(model);

    // When & Then
    mockMvc
        .perform(get("/api/v1/admin/audit-logs/" + seqId).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.seqId").value(seqId))
        .andExpect(jsonPath("$.action").value("SECRET_READ"));
  }
}
