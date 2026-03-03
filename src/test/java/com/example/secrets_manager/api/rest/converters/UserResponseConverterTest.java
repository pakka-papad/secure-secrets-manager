package com.example.secrets_manager.api.rest.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.api.rest.dto.UserResponse;
import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.models.UserRole;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserResponseConverterTest {

  @Test
  void fromModel_ShouldMapAllFieldsCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    User model =
        User.builder()
            .id(id)
            .name("testUser")
            .roles(EnumSet.of(UserRole.ADMIN, UserRole.USER))
            .createdAt(now)
            .modifiedAt(now)
            .build();

    // When
    UserResponse response = UserResponseConverter.fromModel(model);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(id);
    assertThat(response.getName()).isEqualTo("testUser");
    assertThat(response.getRoles()).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.USER);
    assertThat(response.getCreatedAt()).isEqualTo(now);
    assertThat(response.getModifiedAt()).isEqualTo(now);
  }

  @Test
  void fromModel_WithNullModel_ShouldReturnNull() {
    assertThat(UserResponseConverter.fromModel(null)).isNull();
  }

  @Test
  void fromModel_WithNullRoles_ShouldReturnEmptyEnumSetInResponse() {
    // Given
    User model = User.builder().roles(null).build();

    // When
    UserResponse response = UserResponseConverter.fromModel(model);

    // Then
    assertThat(response.getRoles()).isNull();
  }
}
