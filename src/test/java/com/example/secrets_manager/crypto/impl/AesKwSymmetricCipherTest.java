package com.example.secrets_manager.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.secrets_manager.crypto.CipherPurpose;
import com.example.secrets_manager.crypto.SymmetricCipher;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AesKwSymmetricCipherTest {

  private static Stream<Arguments> aesKwCiphers() {
    return Stream.of(
        Arguments.of(new AesKw128SymmetricCipher(), "AES-KW-128", 16),
        Arguments.of(new AesKw192SymmetricCipher(), "AES-KW-192", 24),
        Arguments.of(new AesKw256SymmetricCipher(), "AES-KW-256", 32));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("aesKwCiphers")
  void shouldReturnCorrectMetadata(
      SymmetricCipher cipher, String expectedName, int expectedKeySize) {
    assertThat(cipher.getAlgorithmName()).isEqualTo(expectedName);
    assertThat(cipher.getRequiredKeySizeBytes()).isEqualTo(expectedKeySize);
    assertThat(cipher.getSupportedPurposes()).containsExactly(CipherPurpose.KEY_WRAP);
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("aesKwCiphers")
  void shouldWrapAndUnwrapKey(SymmetricCipher cipher, String expectedName, int keySize)
      throws CryptoOperationException {
    byte[] kek = new byte[keySize];
    byte[] dek = new byte[32]; // Simulate an AES-256 key being wrapped
    for (int i = 0; i < keySize; i++) kek[i] = (byte) i;
    for (int i = 0; i < 32; i++) dek[i] = (byte) (32 - i);

    // Wrap
    EncryptedData wrapped = cipher.encrypt(dek, kek);

    assertThat(wrapped.getAlgorithm()).isEqualTo(expectedName);
    assertThat(wrapped.getNonce()).isNull();
    assertThat(wrapped.getAuthTag()).isNull();
    // RFC 3394: wrapped size = plaintext size + 8 bytes ICV
    assertThat(wrapped.getCiphertext()).hasSize(dek.length + 8);

    // Unwrap
    byte[] unwrapped = cipher.decrypt(wrapped, kek);

    assertThat(unwrapped).isEqualTo(dek);
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("aesKwCiphers")
  void shouldFailUnwrapWithWrongKek(SymmetricCipher cipher, String expectedName, int keySize) {
    byte[] kek = new byte[keySize];
    byte[] dek = new byte[32];
    for (int i = 0; i < keySize; i++) kek[i] = (byte) i;

    EncryptedData wrapped = cipher.encrypt(dek, kek);

    byte[] wrongKek = new byte[keySize];
    wrongKek[0] = (byte) 0xFF;

    assertThatThrownBy(() -> cipher.decrypt(wrapped, wrongKek))
        .isInstanceOf(CryptoOperationException.class)
        .hasMessageContaining("Failed to unwrap key");
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("aesKwCiphers")
  void shouldValidateKeyLength(SymmetricCipher cipher, String expectedName, int keySize) {
    byte[] dek = new byte[32];
    byte[] invalidKey = new byte[keySize - 1];

    assertThatThrownBy(() -> cipher.encrypt(dek, invalidKey))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
