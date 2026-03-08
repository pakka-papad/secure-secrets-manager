package com.example.secrets_manager.core.data.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import com.example.secrets_manager.core.models.SecretGroup;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecretGroupEntityConverterTest {

  @Test
  void toModel_ShouldMapAllFields() {
    // Given
    var id = UUID.randomUUID();
    var now = Instant.now();
    var entity =
        SecretGroupEntity.builder()
            .id(id)
            .name("group1")
            .encryptAlgo("AES-256-GCM")
            .createdAt(now)
            .modifiedAt(now)
            .build();

    // When
    var model = SecretGroupEntityConverter.toModel(entity);

    // Then
    assertThat(model.getId()).isEqualTo(id);
    assertThat(model.getName()).isEqualTo("group1");
    assertThat(model.getEncryptAlgo()).isEqualTo("AES-256-GCM");
    assertThat(model.getCreatedAt()).isEqualTo(now);
  }

  @Test
  void fromModel_ShouldMapAllFields() {
    // Given
    var id = UUID.randomUUID();
    var now = Instant.now();
    var model =
        SecretGroup.builder()
            .id(id)
            .name("group2")
            .encryptAlgo("AES-GCM")
            .createdAt(now)
            .modifiedAt(now)
            .build();

    // When
    var entity = SecretGroupEntityConverter.fromModel(model);

    // Then
    assertThat(entity.getId()).isEqualTo(id);
    assertThat(entity.getName()).isEqualTo("group2");
    assertThat(entity.getEncryptAlgo()).isEqualTo("AES-GCM");
    assertThat(entity.getCreatedAt()).isEqualTo(now);
  }

  @Test
  void toModel_WithNull_ShouldReturnNull() {
    assertThat(SecretGroupEntityConverter.toModel(null)).isNull();
  }

  @Test
  void fromModel_WithNull_ShouldReturnNull() {
    assertThat(SecretGroupEntityConverter.fromModel(null)).isNull();
  }
}
