package com.example.secrets_manager.core.data.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupAuthorizationId;
import com.example.secrets_manager.core.models.PermissionType;
import com.example.secrets_manager.core.models.SecretGroupAuthorization;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecretGroupAuthorizationEntityConverterTest {

  @Test
  void toModel_ShouldMapAllFieldsCorrectly() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    Instant now = Instant.now();
    SecretGroupAuthorizationEntity entity =
        SecretGroupAuthorizationEntity.builder()
            .id(new SecretGroupAuthorizationId(userId, groupId))
            .pRead(true)
            .pWrite(false)
            .pDelete(true)
            .modifiedAt(now)
            .build();

    // When
    SecretGroupAuthorization model = SecretGroupAuthorizationEntityConverter.toModel(entity);

    // Then
    assertThat(model).isNotNull();
    assertThat(model.getUserId()).isEqualTo(userId);
    assertThat(model.getGroupId()).isEqualTo(groupId);
    assertThat(model.getPermissions())
        .containsExactlyInAnyOrder(PermissionType.READ, PermissionType.DELETE);
    assertThat(model.getModifiedAt()).isEqualTo(now);
  }

  @Test
  void fromModel_ShouldMapAllFieldsCorrectly() {
    // Given
    UUID userId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    Instant now = Instant.now();
    SecretGroupAuthorization model =
        SecretGroupAuthorization.builder()
            .userId(userId)
            .groupId(groupId)
            .permissions(EnumSet.of(PermissionType.WRITE))
            .modifiedAt(now)
            .build();

    // When
    SecretGroupAuthorizationEntity entity =
        SecretGroupAuthorizationEntityConverter.fromModel(model);

    // Then
    assertThat(entity).isNotNull();
    assertThat(entity.getId().getUserId()).isEqualTo(userId);
    assertThat(entity.getId().getGroupId()).isEqualTo(groupId);
    assertThat(entity.isPRead()).isFalse();
    assertThat(entity.isPWrite()).isTrue();
    assertThat(entity.isPDelete()).isFalse();
    assertThat(entity.getModifiedAt()).isEqualTo(now);
  }
}
