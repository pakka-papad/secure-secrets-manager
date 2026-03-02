package com.example.secrets_manager.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.api.rest.dto.UserCreationRequest;
import com.example.secrets_manager.api.rest.dto.UserPasswordUpdateRequest;
import com.example.secrets_manager.config.JacksonConfig;
import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.services.JwtTokenService;
import com.example.secrets_manager.core.services.UserService;
import com.example.secrets_manager.core.services.exceptions.InvalidPasswordException;
import com.example.secrets_manager.core.services.exceptions.UserAlreadyExistsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@Import({JacksonConfig.class, UserControllerTest.MethodSecurityConfig.class})
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;
  @MockitoBean private JwtTokenService jwtTokenService;

  @TestConfiguration
  @EnableMethodSecurity
  static class MethodSecurityConfig {}

  @Test
  @WithMockUser(roles = "ADMIN")
  void createUser_AsAdmin_ShouldReturn201() throws Exception {
    // Given
    UserCreationRequest request =
        UserCreationRequest.builder().name("newUser").password("password123").build();

    when(userService.createUser(any()))
        .thenReturn(User.builder().id(UUID.randomUUID()).name("newUser").build());

    // When
    mockMvc
        .perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createUser_WhenAlreadyExists_ShouldReturn409() throws Exception {
    // Given
    UserCreationRequest request =
        UserCreationRequest.builder().name("exists").password("password123").build();

    when(userService.createUser(any()))
        .thenThrow(new UserAlreadyExistsException("User already exists"));

    // When
    mockMvc
        .perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "USER")
  void createUser_AsUser_ShouldReturn403() throws Exception {
    // Given
    UserCreationRequest request = new UserCreationRequest();

    // When
    mockMvc
        .perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser
  void updatePassword_ShouldReturn204() throws Exception {
    // Given
    UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("old", "newPassword123");

    // When
    mockMvc
        .perform(
            put("/api/v1/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void updatePassword_WithWrongOldPassword_ShouldReturn401() throws Exception {
    // Given
    UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("wrong", "newPassword123");

    doThrow(new InvalidPasswordException("Invalid old password"))
        .when(userService)
        .updatePassword(any());

    // When
    mockMvc
        .perform(
            put("/api/v1/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isUnauthorized());
  }
}
