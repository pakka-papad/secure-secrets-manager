package com.example.secrets_manager.api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.secrets_manager.api.rest.dto.LoginRequest;
import com.example.secrets_manager.config.JacksonConfig;
import com.example.secrets_manager.config.TestSecurityConfig;
import com.example.secrets_manager.core.models.AuthResponse;
import com.example.secrets_manager.core.models.RefreshTokenPayload;
import com.example.secrets_manager.core.services.AuthenticationService;
import com.example.secrets_manager.core.services.JwtTokenService;
import com.example.secrets_manager.core.services.exceptions.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(controllers = AuthenticationController.class)
@Import({JacksonConfig.class, TestSecurityConfig.class})
class AuthenticationControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;

  private MockMvc mockMvc;

  @MockitoBean private AuthenticationService authenticationService;
  @MockitoBean private JwtTokenService jwtTokenService;
  @MockitoBean private CacheManager cacheManager;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void login_WithJson_ShouldReturn200() throws Exception {
    // Given
    LoginRequest request = new LoginRequest("user", "password");
    AuthResponse response =
        AuthResponse.builder()
            .accessToken("access")
            .accessTokenExpiresAt(Instant.now().plusSeconds(3600))
            .build();

    when(authenticationService.login(any())).thenReturn(response);

    // When
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").value("access"));
  }

  @Test
  void login_WithFormData_ShouldReturn200() throws Exception {
    // Given
    AuthResponse response = AuthResponse.builder().accessToken("access").build();

    when(authenticationService.login(any())).thenReturn(response);

    // When
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "user")
                .param("password", "password"))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").value("access"));
  }

  @Test
  void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
    // Given
    LoginRequest request = new LoginRequest("user", "wrong_password");

    when(authenticationService.login(any()))
        .thenThrow(new BadCredentialsException("Invalid credentials"));

    // When
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refresh_ShouldReturn200() throws Exception {
    // Given
    RefreshTokenPayload request = new RefreshTokenPayload("valid_refresh_token");
    AuthResponse response =
        AuthResponse.builder().accessToken("new_access").refreshToken("new_refresh").build();

    when(authenticationService.refreshToken(any())).thenReturn(response);

    // When
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").value("new_access"))
        .andExpect(jsonPath("$.refresh_token").value("new_refresh"));
  }

  @Test
  void refresh_WithInvalidToken_ShouldReturn401() throws Exception {
    // Given
    RefreshTokenPayload request = new RefreshTokenPayload("invalid_token");

    when(authenticationService.refreshToken(any()))
        .thenThrow(new InvalidTokenException("Invalid refresh token"));

    // When
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        // Then
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  void logout_ShouldReturn204() throws Exception {
    // When
    mockMvc
        .perform(post("/api/v1/auth/logout"))
        // Then
        .andExpect(status().isNoContent());
  }

  @Test
  void logout_WhenUnauthenticated_ShouldReturn401() throws Exception {
    // When
    mockMvc
        .perform(post("/api/v1/auth/logout"))
        // Then
        .andExpect(status().isUnauthorized());
  }
}
