package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationId;
import com.example.secrets_manager.core.data.repositories.SecretGroupAuthorizationInfo;
import com.example.secrets_manager.core.data.repositories.SecretGroupAuthorizationRepository;
import com.example.secrets_manager.core.data.repositories.SecretGroupRepository;
import com.example.secrets_manager.core.data.repositories.UserRepository;
import com.example.secrets_manager.core.data.repositories.UserRoleInfo;
import com.example.secrets_manager.core.models.ModifyAuthorizationPayload;
import com.example.secrets_manager.core.models.PermissionType;
import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SecretGroupAuthorizationServiceTest {

  @Mock private SecretGroupAuthorizationRepository authorizationRepository;
  @Mock private SecretGroupRepository secretGroupRepository;
  @Mock private UserRepository userRepository;
  @Mock private AuditService auditService;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private SecretGroupAuthorizationService service;

  private MockedStatic<SecurityUtils> mockedSecurityUtils;
  private UUID actorId;
  private UUID targetId;
  private UUID groupId;

  @BeforeEach
  void setUp() {
    actorId = UUID.randomUUID();
    targetId = UUID.randomUUID();
    groupId = UUID.randomUUID();
    mockedSecurityUtils = mockStatic(SecurityUtils.class);
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtils.close();
  }

  @Test
  void modifyAuthorization_AsAdmin_ShouldSucceed() {
    // Given
    var payload =
        new ModifyAuthorizationPayload(targetId, groupId, EnumSet.of(PermissionType.READ));

    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(actorId);
    mockedSecurityUtils.when(() -> SecurityUtils.hasRole(UserRole.ADMIN)).thenReturn(true);
    when(secretGroupRepository.existsByIdAndDeletedAtIsNull(groupId)).thenReturn(true);
    when(userRepository.existsByIdAndAnyRole(eq(targetId), anyList())).thenReturn(true);

    when(authorizationRepository.findAllByIdGroupIdAndIdUserIdIn(eq(groupId), anyList()))
        .thenReturn(Collections.emptyList());
    when(authorizationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    // When
    var result = service.modifyAuthorization(payload);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getPermissions()).containsExactly(PermissionType.READ);
    verify(auditService).save(any());
  }

  @Test
  void modifyAuthorization_AsManager_EscalationBlocked() {
    // Given
    var payload =
        new ModifyAuthorizationPayload(targetId, groupId, EnumSet.of(PermissionType.WRITE));

    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(actorId);
    mockedSecurityUtils.when(() -> SecurityUtils.hasRole(UserRole.ADMIN)).thenReturn(false);
    mockedSecurityUtils.when(() -> SecurityUtils.hasRole(UserRole.SECRET_MANAGER)).thenReturn(true);

    when(secretGroupRepository.existsByIdAndDeletedAtIsNull(groupId)).thenReturn(true);
    when(userRepository.existsByIdAndAnyRole(eq(targetId), anyList())).thenReturn(true);

    // Actor only has READ permission
    var actorAuth =
        SecretGroupAuthorizationEntity.builder()
            .id(new SecretGroupAuthorizationId(actorId, groupId))
            .pRead(true)
            .build();

    when(authorizationRepository.findAllByIdGroupIdAndIdUserIdIn(eq(groupId), anyList()))
        .thenReturn(List.of(actorAuth));

    // When & Then
    assertThrows(AccessDeniedException.class, () -> service.modifyAuthorization(payload));
  }

  @Test
  void modifyAuthorization_TotalRevocation_ShouldDeleteRow() {
    // Given
    var payload =
        new ModifyAuthorizationPayload(targetId, groupId, EnumSet.noneOf(PermissionType.class));

    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(actorId);
    mockedSecurityUtils.when(() -> SecurityUtils.hasRole(UserRole.ADMIN)).thenReturn(true);
    when(secretGroupRepository.existsByIdAndDeletedAtIsNull(groupId)).thenReturn(true);
    when(userRepository.existsByIdAndAnyRole(eq(targetId), anyList())).thenReturn(true);

    var targetAuth =
        SecretGroupAuthorizationEntity.builder()
            .id(new SecretGroupAuthorizationId(targetId, groupId))
            .pRead(true)
            .build();

    when(authorizationRepository.findAllByIdGroupIdAndIdUserIdIn(eq(groupId), anyList()))
        .thenReturn(List.of(targetAuth));

    // When
    var result = service.modifyAuthorization(payload);

    // Then
    assertThat(result).isEmpty();
    verify(authorizationRepository).deleteById(any(SecretGroupAuthorizationId.class));
  }

  @Test
  void getUserAuthorization_ForAdmin_ReturnsVirtualFullAccess() {
    // Given
    when(secretGroupRepository.existsByIdAndDeletedAtIsNull(groupId)).thenReturn(true);

    var roleInfo = mock(UserRoleInfo.class);
    when(roleInfo.getName()).thenReturn("admin-user");
    when(roleInfo.getRoles()).thenReturn(new String[] {"ADMIN", "USER"});
    when(userRepository.findRoleInfoById(targetId)).thenReturn(Optional.of(roleInfo));

    // When
    var result = service.getUserAuthorization(groupId, targetId);

    // Then
    assertThat(result.getPermissions()).containsAll(EnumSet.allOf(PermissionType.class));
    assertThat(result.getUsername()).isEqualTo("admin-user");
    assertThat(result.getModifiedAt()).isNull();
    verify(authorizationRepository, never()).findByGroupIdAndUserIdSurgical(any(), any());
  }

  @Test
  void getUserAuthorization_ForStandardUser_ReturnsAclEntry() {
    // Given
    when(secretGroupRepository.existsByIdAndDeletedAtIsNull(groupId)).thenReturn(true);

    var roleInfo = mock(UserRoleInfo.class);
    when(roleInfo.getRoles()).thenReturn(new String[] {"USER"});
    when(userRepository.findRoleInfoById(targetId)).thenReturn(Optional.of(roleInfo));

    var authInfo = mock(SecretGroupAuthorizationInfo.class);
    when(authInfo.getUserId()).thenReturn(targetId);
    when(authInfo.getUsername()).thenReturn("std-user");
    when(authInfo.isPRead()).thenReturn(true);

    when(authorizationRepository.findByGroupIdAndUserIdSurgical(groupId, targetId))
        .thenReturn(Optional.of(authInfo));

    // When
    var result = service.getUserAuthorization(groupId, targetId);

    // Then
    assertThat(result.getPermissions()).containsExactly(PermissionType.READ);
    assertThat(result.getUsername()).isEqualTo("std-user");
  }

  @Test
  void validateUser_SystemUser_ThrowsException() {
    // Given
    UUID systemUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    var payload =
        new ModifyAuthorizationPayload(systemUserId, groupId, EnumSet.of(PermissionType.READ));

    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(actorId);
    when(secretGroupRepository.existsByIdAndDeletedAtIsNull(groupId)).thenReturn(true);

    // Repository is refactored to return false for system user
    when(userRepository.existsByIdAndAnyRole(eq(systemUserId), any())).thenReturn(false);

    // When & Then
    assertThrows(EntityNotFoundException.class, () -> service.modifyAuthorization(payload));
  }
}
