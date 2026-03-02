package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.data.repositories.RefreshTokenRepository;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.models.UserCreationPayload;
import com.example.secrets_manager.core.models.UserPasswordUpdatePayload;
import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.core.services.exceptions.InvalidPasswordException;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.example.secrets_manager.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private CryptographyService cryptographyService;
  @Mock private AuditService auditService;
  @Mock private SecurityEventLogService securityEventLogService;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private UserService userService;

  private HashedPassword mockHashedPassword;
  private UserEntity mockUserEntity;
  private UUID adminId;
  private UUID userId;
  private MockedStatic<SecurityUtils> mockedSecurityUtils;

  @BeforeEach
  void setUp() {
    adminId = UUID.randomUUID();
    userId = UUID.randomUUID();
    mockedSecurityUtils = mockStatic(SecurityUtils.class);

    mockHashedPassword =
        new HashedPassword(
            new byte[] {4, 5, 6}, new byte[] {1, 2, 3}, "BCRYPT", Map.of("rounds", 12));

    mockUserEntity =
        UserEntity.builder()
            .id(userId)
            .name("testuser")
            .pwSalt(mockHashedPassword.getSalt())
            .pwDigest(mockHashedPassword.getDigest())
            .hashAlgo(mockHashedPassword.getAlgorithm())
            .hashParams("{}")
            .roles(new String[] {"USER"})
            .build();
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtils.close();
  }

  @Test
  void createUser_shouldCreateUserAndAuditLog() throws Exception {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);
    var payload =
        UserCreationPayload.builder()
            .name("testuser")
            .password("password".getBytes())
            .roles(EnumSet.of(UserRole.USER))
            .build();

    when(cryptographyService.hashPassword(any())).thenReturn(mockHashedPassword);
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity);

    // When
    User result = userService.createUser(payload);

    // Then
    assertThat(result).isNotNull();
    verify(userRepository).save(any(UserEntity.class));

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.USER_CREATE);
    assertThat(auditCaptor.getValue().getActorUserId()).isEqualTo(adminId);
  }

  @Test
  void updatePassword_shouldUpdatePasswordAndGlobalLogout() throws Exception {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
    var payload =
        UserPasswordUpdatePayload.builder()
            .oldPassword("old_password".getBytes())
            .newPassword("new_password".getBytes())
            .build();

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockUserEntity));
    when(cryptographyService.verifyPassword(any(), any())).thenReturn(true);
    when(cryptographyService.hashPassword(payload.getNewPassword())).thenReturn(mockHashedPassword);
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");

    // When
    userService.updatePassword(payload);

    // Then
    verify(userRepository).save(mockUserEntity);
    verify(refreshTokenRepository).deleteByUserId(userId);

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.USER_PASSWORD_UPDATE);
  }

  @Test
  void updatePassword_shouldThrowInvalidPasswordException_whenOldPasswordMismatch() {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
    var payload =
        UserPasswordUpdatePayload.builder()
            .oldPassword("wrong".getBytes())
            .newPassword("new".getBytes())
            .build();

    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockUserEntity));
    when(cryptographyService.verifyPassword(any(), any())).thenReturn(false);

    // When & Then
    assertThrows(InvalidPasswordException.class, () -> userService.updatePassword(payload));
    verify(refreshTokenRepository, never()).deleteByUserId(any());
  }
}
