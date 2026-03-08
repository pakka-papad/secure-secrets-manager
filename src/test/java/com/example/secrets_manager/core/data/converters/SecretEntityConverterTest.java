package com.example.secrets_manager.core.data.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.data.entities.MasterKeyEntity;
import com.example.secrets_manager.core.data.entities.SecretEntity;
import com.example.secrets_manager.core.data.entities.SecretGroupEntity;
import com.example.secrets_manager.core.models.Secret;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecretEntityConverterTest {

  @Test
  void toModel_ShouldMapAllFieldsAndExtractAlgorithms() {
    // Given
    var groupId = UUID.randomUUID();
    var group = SecretGroupEntity.builder().id(groupId).encryptAlgo("AES-SECRET").build();
    var masterKey = MasterKeyEntity.builder().version(1).encryptAlgo("AES-MK").build();

    var entity =
        SecretEntity.builder()
            .id(UUID.randomUUID())
            .groupId(groupId)
            .secretName("my-secret")
            .valueCiphertext(new byte[] {1})
            .valueNonce(new byte[] {2})
            .valueAuthTag(new byte[] {3})
            .dekCiphertext(new byte[] {4})
            .dekNonce(new byte[] {5})
            .dekAuthTag(new byte[] {6})
            .dekVersion(10)
            .masterKeyVersion(1)
            .group(group) // Simulated Join
            .masterKey(masterKey) // Simulated Join
            .build();

    // When
    var model = SecretEntityConverter.toModel(entity);

    // Then
    assertThat(model.getSecretName()).isEqualTo("my-secret");
    assertThat(model.getDekVersion()).isEqualTo(10);
    assertThat(model.getMasterKeyVersion()).isEqualTo(1);

    // Verify Value Envelope
    assertThat(model.getValueEnvelope().getCiphertext()).containsExactly(1);
    assertThat(model.getValueEnvelope().getNonce()).containsExactly(2);
    assertThat(model.getValueEnvelope().getAuthTag()).containsExactly(3);
    assertThat(model.getValueEnvelope().getAlgorithm()).isEqualTo("AES-SECRET");

    // Verify DEK Envelope
    assertThat(model.getDekEnvelope().getCiphertext()).containsExactly(4);
    assertThat(model.getDekEnvelope().getNonce()).containsExactly(5);
    assertThat(model.getDekEnvelope().getAuthTag()).containsExactly(6);
    assertThat(model.getDekEnvelope().getAlgorithm()).isEqualTo("AES-MK");
  }

  @Test
  void fromModel_ShouldFlattenEnvelopesToColumns() {
    // Given
    var valueEnv = new EncryptedData(new byte[] {1}, new byte[] {2}, new byte[] {3}, "ignored");
    var dekEnv = new EncryptedData(new byte[] {4}, new byte[] {5}, new byte[] {6}, "ignored");

    var model =
        Secret.builder()
            .id(UUID.randomUUID())
            .groupId(UUID.randomUUID())
            .secretName("to-entity")
            .valueEnvelope(valueEnv)
            .dekEnvelope(dekEnv)
            .dekVersion(20)
            .masterKeyVersion(2)
            .build();

    // When
    var entity = SecretEntityConverter.fromModel(model);

    // Then
    assertThat(entity.getSecretName()).isEqualTo("to-entity");
    assertThat(entity.getValueCiphertext()).containsExactly(1);
    assertThat(entity.getValueNonce()).containsExactly(2);
    assertThat(entity.getValueAuthTag()).containsExactly(3);
    assertThat(entity.getDekCiphertext()).containsExactly(4);
    assertThat(entity.getDekNonce()).containsExactly(5);
    assertThat(entity.getDekAuthTag()).containsExactly(6);
    assertThat(entity.getDekVersion()).isEqualTo(20);
    assertThat(entity.getMasterKeyVersion()).isEqualTo(2);
  }

  @Test
  void toModel_WithNull_ShouldReturnNull() {
    assertThat(SecretEntityConverter.toModel(null)).isNull();
  }

  @Test
  void fromModel_WithNull_ShouldReturnNull() {
    assertThat(SecretEntityConverter.fromModel(null)).isNull();
  }
}
