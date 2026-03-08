package com.example.secrets_manager.core.data.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import com.example.secrets_manager.core.models.MasterKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MasterKeyEntityConverterTest {

  @Test
  void toModel_ShouldMapAllFields() {
    // Given
    var now = Instant.now();
    var entity =
        MasterKeyEntity.builder()
            .version(1)
            .status("ACTIVE")
            .encryptAlgo("AES-256-GCM")
            .createdAt(now)
            .build();

    // When
    var model = MasterKeyEntityConverter.toModel(entity);

    // Then
    assertThat(model.getVersion()).isEqualTo(1);
    assertThat(model.getStatus()).isEqualTo("ACTIVE");
    assertThat(model.getEncryptAlgo()).isEqualTo("AES-256-GCM");
    assertThat(model.getCreatedAt()).isEqualTo(now);
  }

  @Test
  void fromModel_ShouldMapAllFields() {
    // Given
    var now = Instant.now();
    var model =
        MasterKey.builder()
            .version(2)
            .status("RETIRED")
            .encryptAlgo("AES-GCM")
            .createdAt(now)
            .build();

    // When
    var entity = MasterKeyEntityConverter.fromModel(model);

    // Then
    assertThat(entity.getVersion()).isEqualTo(2);
    assertThat(entity.getStatus()).isEqualTo("RETIRED");
    assertThat(entity.getEncryptAlgo()).isEqualTo("AES-GCM");
    assertThat(entity.getCreatedAt()).isEqualTo(now);
  }

  @Test
  void toModel_WithNull_ShouldReturnNull() {
    assertThat(MasterKeyEntityConverter.toModel(null)).isNull();
  }

  @Test
  void fromModel_WithNull_ShouldReturnNull() {
    assertThat(MasterKeyEntityConverter.fromModel(null)).isNull();
  }
}
