package com.example.secrets_manager.core.data.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.data.entities.UserEntity;
import com.example.secrets_manager.core.models.User;
import com.example.secrets_manager.core.models.UserRole;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserEntityConverterTest {

  @Test
  void toModel_ShouldMapAllFieldsCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    UserEntity entity =
        UserEntity.builder()
            .id(id)
            .name("testUser")
            .pwSalt(new byte[] {1, 2})
            .pwDigest(new byte[] {3, 4})
            .createdAt(now)
            .modifiedAt(now)
            .hashAlgo("BCRYPT")
            .hashParams("{}")
            .roles(new String[] {"ADMIN", "USER"})
            .deletedAt(null)
            .build();

    // When
    User model = UserEntityConverter.toModel(entity);

    // Then
    assertThat(model).isNotNull();
    assertThat(model.getId()).isEqualTo(id);
    assertThat(model.getName()).isEqualTo("testUser");
    assertThat(model.getRoles()).containsExactlyInAnyOrder(UserRole.ADMIN, UserRole.USER);
    assertThat(model.getHashAlgo()).isEqualTo("BCRYPT");
  }

  @Test
  void fromModel_ShouldMapAllFieldsCorrectly() {
    // Given
    UUID id = UUID.randomUUID();
    User model =
        User.builder()
            .id(id)
            .name("testUser")
            .roles(EnumSet.of(UserRole.ADMIN, UserRole.USER))
            .hashAlgo("BCRYPT")
            .hashParams("{}")
            .build();

    // When
    UserEntity entity = UserEntityConverter.fromModel(model);

    // Then
    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getName()).isEqualTo("testUser");
    assertThat(entity.getRoles()).containsExactlyInAnyOrder("ADMIN", "USER");
  }

  @Test
  void toModel_WithNullRoles_ShouldReturnEmptyEnumSet() {
    // Given
    UserEntity entity = UserEntity.builder().roles(null).build();

    // When
    User model = UserEntityConverter.toModel(entity);

    // Then
    assertThat(model.getRoles()).isEmpty();
  }

  @Test
  void fromModel_WithNullRoles_ShouldReturnEmptyArray() {
    // Given
    User model = User.builder().roles(null).build();

    // When
    UserEntity entity = UserEntityConverter.fromModel(model);

    // Then
    assertThat(entity.getRoles()).isEmpty();
  }
}
