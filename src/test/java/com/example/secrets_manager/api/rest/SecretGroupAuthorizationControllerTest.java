package com.example.secrets_manager.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.api.rest.dto.ModifyAuthorizationRequest;
import com.example.secrets_manager.config.JacksonConfig;
import com.example.secrets_manager.config.TestSecurityConfig;
import com.example.secrets_manager.core.components.SecretGroupSecurityEvaluator;
import com.example.secrets_manager.core.models.PermissionType;
import com.example.secrets_manager.core.models.SecretGroupAuthorization;
import com.example.secrets_manager.core.models.SecretGroupAuthorizationDetailed;
import com.example.secrets_manager.core.services.JwtTokenService;
import com.example.secrets_manager.core.services.SecretGroupAuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(controllers = SecretGroupAuthorizationController.class)
@Import({JacksonConfig.class, TestSecurityConfig.class})
class SecretGroupAuthorizationControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;
  private MockMvc mockMvc;

  @MockitoBean private SecretGroupAuthorizationService authorizationService;
  @MockitoBean private JwtTokenService jwtTokenService;
  @MockitoBean private CacheManager cacheManager;

  @MockitoBean(name = "groupAuth")
  private SecretGroupSecurityEvaluator groupAuth;

  private UUID groupId;
  private UUID userId;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    groupId = UUID.randomUUID();
    userId = UUID.randomUUID();
  }

  @Test
  @WithMockUser
  void modifyAuthorization_ShouldReturn200_WhenUpdated() throws Exception {
    // Given
    var request = new ModifyAuthorizationRequest(Set.of(PermissionType.READ));
    var model =
        SecretGroupAuthorization.builder()
            .userId(userId)
            .groupId(groupId)
            .permissions(EnumSet.of(PermissionType.READ))
            .modifiedAt(Instant.now())
            .build();

    when(authorizationService.modifyAuthorization(any())).thenReturn(Optional.of(model));

    // When
    mockMvc
        .perform(
            put("/api/v1/secret-groups/{groupId}/authorizations/{userId}", groupId, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.permissions[0]").value("READ"));
  }

  @Test
  @WithMockUser
  void modifyAuthorization_ShouldReturn204_WhenRevoked() throws Exception {
    // Given
    var request = new ModifyAuthorizationRequest(Set.of());
    when(authorizationService.modifyAuthorization(any())).thenReturn(Optional.empty());

    // When
    mockMvc
        .perform(
            put("/api/v1/secret-groups/{groupId}/authorizations/{userId}", groupId, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void listAuthorizations_ShouldReturnPagedResponse() throws Exception {
    // Given
    var detailed =
        SecretGroupAuthorizationDetailed.builder()
            .userId(userId)
            .username("test-user")
            .groupId(groupId)
            .permissions(EnumSet.of(PermissionType.READ))
            .modifiedAt(Instant.now())
            .build();

    var page = new PageImpl<>(List.of(detailed));
    when(authorizationService.listAuthorizations(eq(groupId), any(Pageable.class)))
        .thenReturn(page);

    // When
    mockMvc
        .perform(get("/api/v1/secret-groups/{groupId}/authorizations", groupId))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].username").value("test-user"))
        .andExpect(jsonPath("$.items[0].userId").value(userId.toString()));
  }

  @Test
  @WithMockUser
  void getUserAuthorization_ShouldReturnDetailedResponse() throws Exception {
    // Given
    var detailed =
        SecretGroupAuthorizationDetailed.builder()
            .userId(userId)
            .username("test-user")
            .groupId(groupId)
            .permissions(EnumSet.of(PermissionType.READ))
            .modifiedAt(Instant.now())
            .build();

    when(authorizationService.getUserAuthorization(groupId, userId)).thenReturn(detailed);

    // When
    mockMvc
        .perform(get("/api/v1/secret-groups/{groupId}/authorizations/{userId}", groupId, userId))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("test-user"))
        .andExpect(jsonPath("$.permissions[0]").value("READ"));
  }

  @Test
  void anyEndpoint_WhenUnauthenticated_ShouldReturn401() throws Exception {
    mockMvc
        .perform(get("/api/v1/secret-groups/{groupId}/authorizations", groupId))
        .andExpect(status().isUnauthorized());
  }
}
