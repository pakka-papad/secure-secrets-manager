package com.example.secrets_manager.api.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.api.rest.dto.SecretGroupCreationRequest;
import com.example.secrets_manager.config.JacksonConfig;
import com.example.secrets_manager.config.TestSecurityConfig;
import com.example.secrets_manager.core.components.SecretGroupSecurityEvaluator;
import com.example.secrets_manager.core.models.SecretGroup;
import com.example.secrets_manager.core.services.JwtTokenService;
import com.example.secrets_manager.core.services.SecretGroupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

@WebMvcTest(controllers = SecretGroupController.class)
@Import({JacksonConfig.class, TestSecurityConfig.class})
class SecretGroupControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @MockitoBean private SecretGroupService secretGroupService;
  @MockitoBean private JwtTokenService jwtTokenService;
  @MockitoBean private CacheManager cacheManager;

  // Required by TestSecurityConfig if evaluator is used in @PreAuthorize
  @MockitoBean(name = "groupAuth")
  private SecretGroupSecurityEvaluator secretGroupSecurityEvaluator;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "SECRET_MANAGER")
  void createGroup_AsManager_ShouldReturn201() throws Exception {
    // Given
    var request = new SecretGroupCreationRequest("new-group", "AES-256-GCM");
    var response = SecretGroup.builder().id(UUID.randomUUID()).name("new-group").build();

    when(secretGroupService.createGroup(any())).thenReturn(response);

    // When
    mockMvc
        .perform(
            post("/api/v1/secret-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("new-group"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void createGroup_AsUser_ShouldReturn403() throws Exception {
    // Given
    var request = new SecretGroupCreationRequest("fail", "AES");

    // When
    mockMvc
        .perform(
            post("/api/v1/secret-groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser
  void listGroups_WithParams_ShouldPassToService() throws Exception {
    // Given
    var page = new PageImpl<>(List.of(SecretGroup.builder().name("group1").build()));
    when(secretGroupService.listGroups(any())).thenReturn(page);

    // When
    mockMvc
        .perform(
            get("/api/v1/secret-groups")
                .param("page", "2")
                .param("size", "50")
                .param("sort", "name,asc"))
        // Then
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(secretGroupService).listGroups(pageableCaptor.capture());

    assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    assertThat(pageableCaptor.getValue().getSort().getOrderFor("name").isAscending()).isTrue();
  }

  @Test
  @WithMockUser
  void listGroups_WithNoParams_ShouldPassDefaultsToService() throws Exception {
    // Given
    var page = new PageImpl<>(List.of(SecretGroup.builder().name("group1").build()));
    when(secretGroupService.listGroups(any())).thenReturn(page);

    // When
    mockMvc
        .perform(get("/api/v1/secret-groups"))
        // Then
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(secretGroupService).listGroups(pageableCaptor.capture());

    assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
  }

  @Test
  @WithMockUser
  void getGroup_WhenAuthorized_ShouldReturn200() throws Exception {
    // Given
    UUID groupId = UUID.randomUUID();
    var response = SecretGroup.builder().id(groupId).name("authorized").build();

    when(secretGroupService.getGroup(groupId)).thenReturn(response);

    // When
    mockMvc
        .perform(get("/api/v1/secret-groups/" + groupId))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("authorized"));
  }

  @Test
  @WithMockUser
  void deleteGroup_ShouldReturn204() throws Exception {
    // Given
    UUID groupId = UUID.randomUUID();

    // When
    mockMvc
        .perform(delete("/api/v1/secret-groups/" + groupId))
        // Then
        .andExpect(status().isNoContent());
  }
}
