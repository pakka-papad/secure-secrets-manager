package com.example.secrets_manager.core.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationId;
import com.example.secrets_manager.core.data.repositories.SecretGroupAuthorizationRepository;
import com.example.secrets_manager.core.models.UserRole;
import java.util.Optional;
import java.util.UUID;

import com.example.secrets_manager.security.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecretGroupSecurityEvaluatorTest {

  @Mock private SecretGroupAuthorizationRepository authorizationRepository;
  @InjectMocks private SecretGroupSecurityEvaluator evaluator;

  private MockedStatic<SecurityUtils> mockedSecurityUtils;
  private UUID userId;
  private UUID groupId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    groupId = UUID.randomUUID();
    mockedSecurityUtils = mockStatic(SecurityUtils.class);
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtils.close();
  }

  @Test
  void admin_ShouldAlwaysHaveAccess() {
    // Given
    mockedSecurityUtils.when(() -> SecurityUtils.hasRole(UserRole.ADMIN)).thenReturn(true);

    // When & Then
    assertThat(evaluator.canRead(userId.toString(), groupId)).isTrue();
    assertThat(evaluator.canWrite(userId.toString(), groupId)).isTrue();
    assertThat(evaluator.canDelete(userId.toString(), groupId)).isTrue();

    // Repository should never be called for Admin
    verifyNoInteractions(authorizationRepository);
  }

  @Test
  void userWithReadPermission_ShouldReturnTrueForCanRead() {
    // Given
    mockedSecurityUtils.when(() -> SecurityUtils.hasRole(UserRole.ADMIN)).thenReturn(false);
    var auth =
        SecretGroupAuthorizationEntity.builder().pRead(true).pWrite(false).pDelete(false).build();

    when(authorizationRepository.findById(any(SecretGroupAuthorizationId.class)))
        .thenReturn(Optional.of(auth));

    // When & Then
    assertThat(evaluator.canRead(userId.toString(), groupId)).isTrue();
    assertThat(evaluator.canWrite(userId.toString(), groupId)).isFalse();
  }

  @Test
  void missingRecord_ShouldDenyAccess() {
    // Given
    mockedSecurityUtils.when(() -> SecurityUtils.hasRole(UserRole.ADMIN)).thenReturn(false);
    when(authorizationRepository.findById(any(SecretGroupAuthorizationId.class)))
        .thenReturn(Optional.empty());

    // When & Then
    assertThat(evaluator.canRead(userId.toString(), groupId)).isFalse();
  }

  @Test
  void invalidUserId_ShouldDenyAccess() {
    // When
    boolean result = evaluator.canRead("not-a-uuid", groupId);

    // Then
    assertThat(result).isFalse();
  }
}
