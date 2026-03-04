package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.UserCreationPayload;
import com.example.secrets_manager.crypto.CryptographyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {UserService.class, UserServiceSecurityTest.SecurityTestConfig.class})
class UserServiceSecurityTest {

  @Autowired private UserService userService;

  @MockitoBean private UserRepository userRepository;
  @MockitoBean private RefreshTokenRepository refreshTokenRepository;
  @MockitoBean private CryptographyService cryptographyService;
  @MockitoBean private AuditService auditService;
  @MockitoBean private SecurityEventLogService securityEventLogService;
  @MockitoBean private ObjectMapper objectMapper;

  @EnableMethodSecurity
  static class SecurityTestConfig {}

  @Test
  @WithMockUser(roles = "USER")
  void createUser_AsUser_ShouldThrowAccessDeniedException() {
    UserCreationPayload payload =
        UserCreationPayload.builder().name("test").password("pass".getBytes()).build();

    assertThatThrownBy(() -> userService.createUser(payload))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @WithMockUser(roles = "USER")
  void updateRoles_AsUser_ShouldThrowAccessDeniedException() {
    UUID targetId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                userService.updateRoles(
                    targetId,
                    EnumSet.noneOf(com.example.secrets_manager.core.models.UserRole.class)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void updatePassword_WhenUnauthenticated_ShouldThrowException() {
    com.example.secrets_manager.core.models.UserPasswordUpdatePayload payload =
        com.example.secrets_manager.core.models.UserPasswordUpdatePayload.builder()
            .oldPassword("old".getBytes())
            .newPassword("new".getBytes())
            .build();

    // No @WithMockUser here means unauthenticated
    assertThatThrownBy(() -> userService.updatePassword(payload))
        .isInstanceOf(
            org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
                .class);
  }
}
