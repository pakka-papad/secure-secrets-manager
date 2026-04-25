package com.example.secrets_manager.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.api.rest.advice.AppExceptionHandler;
import com.example.secrets_manager.api.rest.dto.CreateSecretRequest;
import com.example.secrets_manager.api.rest.dto.UpdateSecretValueRequest;
import com.example.secrets_manager.config.JacksonConfig;
import com.example.secrets_manager.config.TestSecurityConfig;
import com.example.secrets_manager.core.components.SecretGroupSecurityEvaluator;
import com.example.secrets_manager.core.models.Secret;
import com.example.secrets_manager.core.services.JwtTokenService;
import com.example.secrets_manager.core.services.SecretService;
import com.example.secrets_manager.core.services.exceptions.SecretAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.SecretServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(controllers = SecretController.class)
@Import({JacksonConfig.class, TestSecurityConfig.class, AppExceptionHandler.class})
class SecretControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @MockitoBean private SecretService secretService;
  @MockitoBean private JwtTokenService jwtTokenService;
  @MockitoBean private CacheManager cacheManager;

  @MockitoBean(name = "groupAuth")
  private SecretGroupSecurityEvaluator secretGroupSecurityEvaluator;

  private final UUID groupId = UUID.randomUUID();

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser
  void createSecret_ShouldReturn201() throws Exception {
    // Given
    var request = new CreateSecretRequest("my-secret", "sensitive-val");
    var response =
        Secret.builder()
            .id(UUID.randomUUID())
            .secretName("my-secret")
            .createdAt(Instant.now())
            .build();

    when(secretService.createSecret(eq(groupId), any())).thenReturn(response);

    // When
    mockMvc
        .perform(
            post("/api/v1/secret-groups/{groupId}/secrets", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("my-secret"));
  }

  @Test
  @WithMockUser
  void createSecret_WhenAlreadyExists_ShouldReturn409() throws Exception {
    // Given
    var request = new CreateSecretRequest("duplicate", "val");
    when(secretService.createSecret(eq(groupId), any()))
        .thenThrow(new SecretAlreadyExistsException("Secret already exists"));

    // When
    mockMvc
        .perform(
            post("/api/v1/secret-groups/{groupId}/secrets", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser
  void listSecrets_WithPagination_ShouldReturnPagedResponse() throws Exception {
    // Given
    var secret = Secret.builder().secretName("s1").build();
    var page = new PageImpl<>(List.of(secret));

    when(secretService.listSecrets(eq(groupId), any(), any())).thenReturn(page);

    // When
    mockMvc
        .perform(
            get("/api/v1/secret-groups/{groupId}/secrets", groupId)
                .param("page", "1")
                .param("size", "10")
                .param("namePrefix", "s"))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].name").value("s1"));
  }

  @Test
  @WithMockUser
  void getSecretMetadata_ShouldReturn200() throws Exception {
    // Given
    String name = "my-secret";
    var response = Secret.builder().secretName(name).build();
    when(secretService.getSecretMetadata(groupId, name)).thenReturn(response);

    // When
    mockMvc
        .perform(get("/api/v1/secret-groups/{groupId}/secrets/{name}", groupId, name))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(name));
  }

  @Test
  @WithMockUser
  void getSecretValue_ShouldReturnDecryptedValue() throws Exception {
    // Given
    String name = "reveal-me";
    String plaintext = "unlocked-data";
    when(secretService.getSecretValue(groupId, name)).thenReturn(plaintext);

    // When
    mockMvc
        .perform(get("/api/v1/secret-groups/{groupId}/secrets/{name}/value", groupId, name))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plaintextValue").value(plaintext));
  }

  @Test
  @WithMockUser
  void getSecretValue_WhenDecryptionFails_ShouldReturn500() throws Exception {
    // Given
    String name = "bad-data";
    when(secretService.getSecretValue(groupId, name))
        .thenThrow(new SecretServiceException("Decryption failed"));

    // When
    mockMvc
        .perform(get("/api/v1/secret-groups/{groupId}/secrets/{name}/value", groupId, name))
        // Then
        .andExpect(status().isInternalServerError());
  }

  @Test
  @WithMockUser
  void updateSecretValue_ShouldReturn200() throws Exception {
    // Given
    String name = "update-me";
    var request = new UpdateSecretValueRequest("new-plain");
    var response = Secret.builder().secretName(name).modifiedAt(Instant.now()).build();

    when(secretService.updateSecretValue(eq(groupId), eq(name), any())).thenReturn(response);

    // When
    mockMvc
        .perform(
            put("/api/v1/secret-groups/{groupId}/secrets/{name}/value", groupId, name)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void deleteSecret_ShouldReturn204() throws Exception {
    // Given
    String name = "to-delete";

    // When
    mockMvc
        .perform(delete("/api/v1/secret-groups/{groupId}/secrets/{name}", groupId, name))
        // Then
        .andExpect(status().isNoContent());

    verify(secretService).deleteSecret(groupId, name);
  }

  @Test
  @WithMockUser
  void getSecretMetadata_WhenNotFound_ShouldReturn404() throws Exception {
    // Given
    String name = "ghost";
    when(secretService.getSecretMetadata(groupId, name))
        .thenThrow(new EntityNotFoundException("secret group not found"));

    // When
    mockMvc
        .perform(get("/api/v1/secret-groups/{groupId}/secrets/{name}", groupId, name))
        // Then
        .andExpect(status().isNotFound());
  }
}
