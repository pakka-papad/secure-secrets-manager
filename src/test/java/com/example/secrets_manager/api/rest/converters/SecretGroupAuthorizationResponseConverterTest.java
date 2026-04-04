package com.example.secrets_manager.api.rest.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationDetailedResponse;
import com.example.secrets_manager.api.rest.dto.SecretGroupAuthorizationResponse;
import com.example.secrets_manager.core.models.PermissionType;
import com.example.secrets_manager.core.models.SecretGroupAuthorization;
import com.example.secrets_manager.core.models.SecretGroupAuthorizationDetailed;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecretGroupAuthorizationResponseConverterTest {

  @Test
  void fromModel_ShouldMapAllFields() {
    // Given
    var model =
        SecretGroupAuthorization.builder()
            .userId(UUID.randomUUID())
            .groupId(UUID.randomUUID())
            .permissions(EnumSet.of(PermissionType.READ, PermissionType.WRITE))
            .modifiedAt(Instant.now())
            .build();

    // When
    SecretGroupAuthorizationResponse response =
        SecretGroupAuthorizationResponseConverter.fromModel(model);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getUserId()).isEqualTo(model.getUserId());
    assertThat(response.getGroupId()).isEqualTo(model.getGroupId());
    assertThat(response.getPermissions()).isEqualTo(model.getPermissions());
    assertThat(response.getModifiedAt()).isEqualTo(model.getModifiedAt());
  }

  @Test
  void fromDetailedModel_ShouldIncludeUsername() {
    // Given
    var model =
        SecretGroupAuthorizationDetailed.builder()
            .userId(UUID.randomUUID())
            .username("test-user")
            .groupId(UUID.randomUUID())
            .permissions(EnumSet.of(PermissionType.READ))
            .modifiedAt(Instant.now())
            .build();

    // When
    SecretGroupAuthorizationDetailedResponse response =
        SecretGroupAuthorizationResponseConverter.fromDetailedModel(model);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getUsername()).isEqualTo("test-user");
    assertThat(response.getUserId()).isEqualTo(model.getUserId());
    assertThat(response.getPermissions()).containsExactly(PermissionType.READ);
  }

  @Test
  void fromModel_WithNull_ShouldReturnNull() {
    assertThat(SecretGroupAuthorizationResponseConverter.fromModel(null)).isNull();
    assertThat(SecretGroupAuthorizationResponseConverter.fromDetailedModel(null)).isNull();
  }
}
