package com.example.secrets_manager.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import com.example.secrets_manager.core.data.repositories.SecretGroupRepository;
import com.example.secrets_manager.core.data.repositories.SecretRepository;
import com.example.secrets_manager.core.models.SecretGroupCreationPayload;
import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.core.services.exceptions.SecretGroupAlreadyExistsException;
import com.example.secrets_manager.crypto.CipherPurpose;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.security.SecurityUtils;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class SecretGroupServiceTest {

  @Mock private SecretGroupRepository secretGroupRepository;
  @Mock private SecretRepository secretRepository;
  @Mock private SecretGroupAuthorizationService authorizationService;
  @Mock private CryptographyService cryptographyService;
  @Mock private AuditService auditService;

  @InjectMocks private SecretGroupService secretGroupService;

  private MockedStatic<SecurityUtils> mockedSecurityUtils;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    mockedSecurityUtils = mockStatic(SecurityUtils.class);
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtils.close();
  }

  @Test
  void createGroup_ShouldSucceed() {
    // Given
    var payload = new SecretGroupCreationPayload("my-group", "AES-256-GCM");
    var entity = SecretGroupEntity.builder().id(UUID.randomUUID()).name("my-group").build();

    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
    when(cryptographyService.isAlgorithmSupported(eq("AES-256-GCM"), eq(CipherPurpose.DATA)))
        .thenReturn(true);
    when(secretGroupRepository.saveAndFlush(any())).thenReturn(entity);

    // When
    var result = secretGroupService.createGroup(payload);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("my-group");
    verify(authorizationService).grantInitialPermissionsInternal(eq(userId), eq(entity.getId()));
    verify(auditService).save(any());
  }

  @Test
  void createGroup_WithDuplicateName_ShouldThrowException() {
    // Given
    var payload = new SecretGroupCreationPayload("exists", "AES-GCM");
    when(cryptographyService.isAlgorithmSupported(any(), eq(CipherPurpose.DATA))).thenReturn(true);
    when(secretGroupRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("uq_sm_secret_groups_active_name"));

    // When & Then
    assertThrows(
        SecretGroupAlreadyExistsException.class, () -> secretGroupService.createGroup(payload));
  }

  @Test
  void listGroups_ShouldReturnPagedGroups() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);
    var entity = SecretGroupEntity.builder().name("group1").build();
    var page = new PageImpl<>(List.of(entity));

    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);
    mockedSecurityUtils.when(() -> SecurityUtils.hasRole(UserRole.ADMIN)).thenReturn(false);
    when(secretGroupRepository.findAuthorizedGroups(eq(userId), eq(pageable))).thenReturn(page);

    // When
    var result = secretGroupService.listGroups(pageable);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().get(0).getName()).isEqualTo("group1");
  }

  @Test
  void listGroups_WithInvalidSort_ShouldThrowException() {
    // Given
    Pageable pageable = PageRequest.of(0, 10, Sort.by("invalid_field"));

    // When & Then
    assertThrows(IllegalArgumentException.class, () -> secretGroupService.listGroups(pageable));
  }

  @Test
  void deleteGroup_ShouldSucceed_WhenNoActiveSecrets() {
    // Given
    UUID groupId = UUID.randomUUID();
    var entity = SecretGroupEntity.builder().id(groupId).build();

    when(secretGroupRepository.findByIdAndDeletedAtIsNull(groupId)).thenReturn(Optional.of(entity));
    when(secretRepository.countByGroupIdAndDeletedAtIsNull(groupId)).thenReturn(0L);
    mockedSecurityUtils.when(SecurityUtils::getAuthenticatedUserId).thenReturn(userId);

    // When
    secretGroupService.deleteGroup(groupId);

    // Then
    assertThat(entity.getDeletedAt()).isNotNull();
    verify(secretGroupRepository).save(entity);
    verify(auditService).save(any());
  }

  @Test
  void deleteGroup_ShouldThrowException_WhenSecretsExist() {
    // Given
    UUID groupId = UUID.randomUUID();
    var entity = SecretGroupEntity.builder().id(groupId).build();

    when(secretGroupRepository.findByIdAndDeletedAtIsNull(groupId)).thenReturn(Optional.of(entity));
    when(secretRepository.countByGroupIdAndDeletedAtIsNull(groupId)).thenReturn(5L);

    // When & Then
    assertThrows(IllegalStateException.class, () -> secretGroupService.deleteGroup(groupId));
  }
}
