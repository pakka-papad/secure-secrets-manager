package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.models.*;
import com.example.secrets_manager.core.models.events.UserDeletedEvent;
import com.example.secrets_manager.core.models.events.UserPasswordUpdatedEvent;
import com.example.secrets_manager.core.models.search.UserSearchCriteria;
import com.example.secrets_manager.core.services.exceptions.*;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.example.secrets_manager.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.EnumSet;
import java.util.List;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private CryptographyService cryptographyService;
  @Mock private AuditService auditService;
  @Mock private SecurityEventLogService securityEventLogService;
  @Mock private SystemLockService systemLockService;
  @Mock private ApplicationEventPublisher eventPublisher;
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
    when(userRepository.saveAndFlush(any(UserEntity.class))).thenReturn(mockUserEntity);

    // When
    User result = userService.createUser(payload);

    // Then
    assertThat(result).isNotNull();
    verify(userRepository).saveAndFlush(any(UserEntity.class));

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.USER_CREATE);
    assertThat(auditCaptor.getValue().getActorUserId()).isEqualTo(adminId);
  }

  @Test
  void createUser_shouldCreateAdminUser_whenRolesIncludeAdmin() throws Exception {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);
    var payload =
        UserCreationPayload.builder()
            .name("newAdmin")
            .password("password".getBytes())
            .roles(EnumSet.of(UserRole.ADMIN, UserRole.USER))
            .build();

    UserEntity adminEntity =
        UserEntity.builder()
            .id(UUID.randomUUID())
            .name("newAdmin")
            .roles(new String[] {"ADMIN", "USER"})
            .build();

    when(cryptographyService.hashPassword(any())).thenReturn(mockHashedPassword);
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    when(userRepository.saveAndFlush(any(UserEntity.class))).thenReturn(adminEntity);

    // When
    User result = userService.createUser(payload);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getRoles()).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.USER);

    ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).saveAndFlush(entityCaptor.capture());
    assertThat(entityCaptor.getValue().getRoles()).containsExactlyInAnyOrder("ADMIN", "USER");
  }

  @Test
  void updatePassword_shouldUpdatePasswordAndPublishEvent() throws Exception {
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
    when(userRepository.save(any())).thenReturn(mockUserEntity);

    // When
    User result = userService.updatePassword(payload);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(mockUserEntity.getName());
    verify(userRepository).save(mockUserEntity);
    verify(eventPublisher).publishEvent(any(UserPasswordUpdatedEvent.class));

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
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void updateRoles_shouldUpdateRolesAndAudit_whenPromotion_skipsGlobalLock() {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);
    EnumSet<UserRole> newRoles =
        EnumSet.of(UserRole.ADMIN, UserRole.USER); // Includes ADMIN (Promotion/Keep)

    when(userRepository.findAndLockById(userId)).thenReturn(Optional.of(mockUserEntity));
    when(userRepository.save(any())).thenReturn(mockUserEntity);

    // When
    User result = userService.updateRoles(userId, newRoles);

    // Then
    assertThat(result).isNotNull();
    verify(systemLockService, never()).acquireExclusiveLock(any());
    verify(userRepository).save(mockUserEntity);
  }

  @Test
  void updateRoles_shouldAcquireGlobalLock_whenPotentialDemotion() {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);
    EnumSet<UserRole> newRoles = EnumSet.of(UserRole.USER); // No ADMIN (Potential demotion)

    UserEntity currentAdmin =
        UserEntity.builder().id(userId).roles(new String[] {"ADMIN", "USER"}).build();

    when(userRepository.findAndLockById(userId)).thenReturn(Optional.of(currentAdmin));
    when(userRepository.countActiveAdmins()).thenReturn(2L);
    when(userRepository.save(any())).thenReturn(currentAdmin);

    // When
    userService.updateRoles(userId, newRoles);

    // Then
    verify(systemLockService).acquireExclusiveLock(SystemLockName.USER_ROLE_MANAGEMENT);
    verify(userRepository).save(currentAdmin);
  }

  @Test
  void createUser_shouldAlwaysIncludeUserRole() throws Exception {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);
    var payload =
        UserCreationPayload.builder()
            .name("testuser") // same as the mockUserEntity
            .password("password".getBytes())
            .roles(EnumSet.noneOf(UserRole.class)) // Empty roles
            .build();

    when(cryptographyService.hashPassword(any())).thenReturn(mockHashedPassword);
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    when(userRepository.saveAndFlush(any())).thenReturn(mockUserEntity);

    // When
    final var result = userService.createUser(payload);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("testuser");
    assertThat(result.getRoles()).containsExactlyInAnyOrder(UserRole.USER);

    ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).saveAndFlush(entityCaptor.capture());
    assertThat(entityCaptor.getValue().getRoles()).contains("USER");
  }

  @Test
  void updateRoles_shouldAlwaysIncludeUserRole() {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);
    EnumSet<UserRole> onlyAdmin = EnumSet.of(UserRole.ADMIN); // USER omitted

    when(userRepository.findAndLockById(userId)).thenReturn(Optional.of(mockUserEntity));
    when(userRepository.save(any())).thenReturn(mockUserEntity);

    // When
    final var result = userService.updateRoles(userId, onlyAdmin);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(mockUserEntity.getName());
    assertThat(result.getRoles()).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.USER);

    ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
    verify(userRepository).save(entityCaptor.capture());
    assertThat(entityCaptor.getValue().getRoles()).contains("USER", "ADMIN");
  }

  @Test
  void updateRoles_shouldThrowAdminDemotionException_whenRemovingLastAdmin() {
    // Given
    UUID anotherUserId = UUID.randomUUID();
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);
    EnumSet<UserRole> newRoles = EnumSet.of(UserRole.USER); // Removing ADMIN

    UserEntity targetAdmin =
        UserEntity.builder().id(anotherUserId).roles(new String[] {"ADMIN"}).build();

    when(userRepository.findAndLockById(anotherUserId)).thenReturn(Optional.of(targetAdmin));
    when(userRepository.countActiveAdmins()).thenReturn(1L);

    // When & Then
    assertThrows(
        AdminDemotionException.class, () -> userService.updateRoles(anotherUserId, newRoles));
    verify(userRepository, never()).save(any());
  }

  @Test
  void updateRoles_shouldThrowSelfDemotionException_whenAdminTargetsSelf() {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);
    EnumSet<UserRole> newRoles = EnumSet.of(UserRole.USER);

    // When & Then
    assertThrows(SelfDemotionException.class, () -> userService.updateRoles(adminId, newRoles));
    verify(userRepository, never()).save(any());
  }

  @Test
  void deleteUser_shouldSoftDeleteAndPublishEvent() {
    // Given
    UUID targetId = UUID.randomUUID();
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);
    UserEntity targetUser = UserEntity.builder().id(targetId).roles(new String[] {"USER"}).build();

    when(userRepository.findAndLockById(targetId)).thenReturn(Optional.of(targetUser));

    // When
    userService.deleteUser(targetId);

    // Then
    verify(userRepository).save(targetUser);
    verify(eventPublisher).publishEvent(new UserDeletedEvent(targetId));
    assertNotNull(targetUser.getDeletedAt());
    assertThat(targetUser.getPwSalt()).isEmpty();
    assertThat(targetUser.getPwDigest()).isEmpty();
    assertThat(targetUser.getHashAlgo()).isEqualTo("SCRUBBED");
    assertThat(targetUser.getHashParams()).isEqualTo("{}");

    ArgumentCaptor<AuditLogPayload> auditCaptor = ArgumentCaptor.forClass(AuditLogPayload.class);
    verify(auditService).save(auditCaptor.capture());
    assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditAction.USER_DELETE);
  }

  @Test
  void deleteUser_shouldThrowSelfDeletionException_whenAdminTargetsSelf() {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(adminId);

    // When & Then
    assertThrows(SelfDeletionException.class, () -> userService.deleteUser(adminId));
    verify(userRepository, never()).save(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void listUsers_shouldReturnPagedUsers() {
    // Given
    UserSearchCriteria criteria = new UserSearchCriteria();
    Pageable pageable = PageRequest.of(0, 10);
    Page<UserEntity> page = new PageImpl<>(List.of(mockUserEntity));

    when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

    // When
    Page<User> result = userService.listUsers(criteria, pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo(mockUserEntity.getName());
  }

  @Test
  void listUsers_WithInvalidSort_ShouldThrowException() {
    // Given
    UserSearchCriteria criteria = new UserSearchCriteria();
    Pageable pageable = PageRequest.of(0, 10, Sort.by("invalidField"));

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> userService.listUsers(criteria, pageable));
  }

  @Test
  void getUserById_shouldReturnUser_whenAdminAndUserExists() {
    // Given
    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockUserEntity));

    // When
    User result = userService.getUserById(userId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getName()).isEqualTo(mockUserEntity.getName());
  }

  @Test
  void getUserById_shouldThrowException_whenUserNotFound() {
    // Given
    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(EntityNotFoundException.class, () -> userService.getUserById(userId));
  }

  @Test
  void getCurrentUser_shouldReturnUser_whenAuthenticated() {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockUserEntity));

    // When
    User result = userService.getCurrentUser();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(userId);
    assertThat(result.getName()).isEqualTo(mockUserEntity.getName());
  }

  @Test
  void getCurrentUser_shouldThrowException_whenUserNotFound() {
    // Given
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
    when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(EntityNotFoundException.class, () -> userService.getCurrentUser());
  }
}
