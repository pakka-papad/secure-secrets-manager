package com.example.secrets_manager.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.api.rest.advice.AppExceptionHandler;
import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.core.services.MasterKeyService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminMasterKeyControllerTest {

  private MockMvc mockMvc;

  @Mock private MasterKeyService masterKeyService;
  @InjectMocks private AdminMasterKeyController controller;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new AppExceptionHandler())
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
  }

  @Test
  void listMasterKeys_ShouldReturnPagedResults() throws Exception {
    // Given
    MasterKey key =
        MasterKey.builder()
            .version(1)
            .status(MasterKeyState.ACTIVE)
            .encryptAlgo("AES-256-GCM")
            .createdAt(Instant.now())
            .build();

    when(masterKeyService.listMasterKeys(any(), any())).thenReturn(new PageImpl<>(List.of(key)));

    // When & Then
    mockMvc
        .perform(get("/api/v1/admin/master-keys").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].version").value(1))
        .andExpect(jsonPath("$.items[0].status").value("ACTIVE"));
  }

  @Test
  void markKeyAsCompromised_ShouldReturnUpdatedKey() throws Exception {
    // Given
    int version = 1;
    MasterKey key =
        MasterKey.builder()
            .version(version)
            .status(MasterKeyState.COMPROMISED)
            .encryptAlgo("AES-256-GCM")
            .createdAt(Instant.now())
            .build();

    when(masterKeyService.markKeyAsCompromised(version)).thenReturn(key);

    // When & Then
    mockMvc
        .perform(post("/api/v1/admin/master-keys/" + version + "/compromise"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(version))
        .andExpect(jsonPath("$.status").value("COMPROMISED"));
  }
}
