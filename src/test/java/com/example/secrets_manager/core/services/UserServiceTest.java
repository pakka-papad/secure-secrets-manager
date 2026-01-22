package com.example.secrets_manager.core.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.AuditAction;
import com.example.secrets_manager.core.models.AuditLogPayload;
import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.models.UserCreationPayload;
import com.example.secrets_manager.core.models.UserPasswordUpdatePayload;
import com.example.secrets_manager.core.services.exceptions.InvalidPasswordException;
import com.example.secrets_manager.core.services.exceptions.UserAlreadyExistsException;
import com.example.secrets_manager.core.services.exceptions.UserServiceException;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private CryptographyService cryptographyService;
  @Mock private AuditService auditService;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private UserService userService;

  private HashedPassword mockHashedPassword;
  private UserEntity mockUserEntity;

  @BeforeEach
  void setUp() {
    mockHashedPassword =
        new HashedPassword(
            new byte[] {4, 5, 6}, new byte[] {1, 2, 3}, "BCRYPT", Map.of("rounds", 12));

    mockUserEntity =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .name("testuser")
            .pwSalt(mockHashedPassword.getSalt())
            .pwDigest(mockHashedPassword.getDigest())
            .hashAlgo(mockHashedPassword.getAlgorithm())
            .hashParams("{}")
            .build();
  }

  @Test
  void createUser_shouldCreateUserAndAuditLog_whenPayloadIsValid() throws Exception {
    // Given
    var payload = new UserCreationPayload("testuser", "password".getBytes());
    when(cryptographyService.hashPassword(any())).thenReturn(mockHashedPassword);
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    when(userRepository.save(any(UserEntity.class))).thenReturn(mockUserEntity);

    // When
    User result = userService.createUser(payload);

    // Then
    assertNotNull(result);
    assertEquals(mockUserEntity.getName(), result.getName());

    verify(cryptographyService, times(1)).hashPassword(payload.getPassword());
    verify(userRepository, times(1)).save(any(UserEntity.class));
    verify(auditService, times(1)).save(any(AuditLogPayload.class));

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    assertEquals(AuditAction.USER_CREATE, auditCaptor.getValue().getAction());
    assertEquals(mockUserEntity.getId(), auditCaptor.getValue().getTargetUserId());
  }

  @Test
  void createUser_shouldThrowUserAlreadyExistsException_whenUserNameIsTaken() {
    // Given
    var payload = new UserCreationPayload("testuser", "password".getBytes());
    when(cryptographyService.hashPassword(any())).thenReturn(mockHashedPassword);
    when(userRepository.save(any(UserEntity.class)))
        .thenThrow(new DataIntegrityViolationException("... uq_sm_users_name ..."));

    // When & Then
    assertThrows(
        UserAlreadyExistsException.class,
        () -> {
          userService.createUser(payload);
        });

    verify(auditService, never()).save(any());
  }

  @Test
  void createUser_shouldThrowUserServiceException_whenJsonSerializationFails() throws Exception {
    // Given
    var payload = new UserCreationPayload("testuser", "password".getBytes());
    when(cryptographyService.hashPassword(any())).thenReturn(mockHashedPassword);
    when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

    // When & Then
    assertThrows(
        UserServiceException.class,
        () -> {
          userService.createUser(payload);
        });

    verify(userRepository, never()).save(any());
    verify(auditService, never()).save(any());
  }

  @Test
  void updatePassword_shouldUpdatePasswordAndAuditLog_whenPayloadIsValid() throws Exception {
    // Given
    var payload =
        new UserPasswordUpdatePayload(
            mockUserEntity.getId(), "old_password".getBytes(), "new_password".getBytes());

    when(userRepository.findByIdAndDeletedAtIsNull(payload.getUserId()))
        .thenReturn(Optional.of(mockUserEntity));
    when(cryptographyService.verifyPassword(any(), any())).thenReturn(true);
    when(cryptographyService.hashPassword(payload.getNewPassword()))
        .thenReturn(
            new HashedPassword(new byte[] {7, 6}, new byte[] {9, 8}, "BCRYPT", Map.of("r", 10)));
    when(objectMapper.writeValueAsString(any())).thenReturn("{\"r\":10}");

    // When
    userService.updatePassword(payload);

    // Then
    verify(userRepository, times(1)).save(mockUserEntity);
    assertEquals("BCRYPT", mockUserEntity.getHashAlgo());
    assertArrayEquals(new byte[] {9, 8}, mockUserEntity.getPwSalt());

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    assertEquals(AuditAction.USER_PASSWORD_UPDATE, auditCaptor.getValue().getAction());
    assertEquals(mockUserEntity.getId(), auditCaptor.getValue().getTargetUserId());
  }

  @Test
  void updatePassword_shouldThrowEntityNotFoundException_whenUserDoesNotExist() {
    // Given
    var payload = new UserPasswordUpdatePayload(UUID.randomUUID(), null, null);
    when(userRepository.findByIdAndDeletedAtIsNull(payload.getUserId()))
        .thenReturn(Optional.empty());

    // When & Then
    assertThrows(
        EntityNotFoundException.class,
        () -> {
          userService.updatePassword(payload);
        });

    verify(userRepository, never()).save(any());
    verify(auditService, never()).save(any());
  }

  @Test
  void updatePassword_shouldThrowInvalidPasswordException_whenOldPasswordIsIncorrect() {
    // Given
    var payload =
        new UserPasswordUpdatePayload(
            mockUserEntity.getId(), "wrong_old_password".getBytes(), "new_password".getBytes());
    when(userRepository.findByIdAndDeletedAtIsNull(payload.getUserId()))
        .thenReturn(Optional.of(mockUserEntity));
    when(cryptographyService.verifyPassword(any(), any())).thenReturn(false);

    // When & Then
    assertThrows(
        InvalidPasswordException.class,
        () -> {
          userService.updatePassword(payload);
        });

    verify(userRepository, never()).save(any());
    verify(auditService, never()).save(any());
  }

  @Test
  void updatePassword_shouldThrowUserServiceException_whenJsonSerializationFails()
      throws Exception {
    // Given
    var payload =
        new UserPasswordUpdatePayload(
            mockUserEntity.getId(), "old_password".getBytes(), "new_password".getBytes());
    when(userRepository.findByIdAndDeletedAtIsNull(payload.getUserId()))
        .thenReturn(Optional.of(mockUserEntity));
    when(cryptographyService.verifyPassword(any(), any())).thenReturn(true);
    when(cryptographyService.hashPassword(payload.getNewPassword())).thenReturn(mockHashedPassword);
    when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

    // When & Then
    assertThrows(
        UserServiceException.class,
        () -> {
          userService.updatePassword(payload);
        });

    verify(userRepository, never()).save(any());
    verify(auditService, never()).save(any());
  }
}
