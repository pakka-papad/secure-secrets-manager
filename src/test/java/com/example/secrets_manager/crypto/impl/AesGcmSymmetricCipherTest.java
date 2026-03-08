package com.example.secrets_manager.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

class AesGcmSymmetricCipherTest {

  private final AesGcmSymmetricCipher cipher = new AesGcmSymmetricCipher();
  private final SecureRandom random = new SecureRandom();

  @Test
  void encryptAndDecrypt_ShouldWorkCorrectly() throws Exception {
    // Given
    byte[] key = new byte[32]; // 256-bit key
    random.nextBytes(key);
    byte[] plaintext = "Secret sensitive data".getBytes();

    // When
    EncryptedData encrypted = cipher.encrypt(plaintext, key);
    assertThat(encrypted).isNotNull();
    assertThat(encrypted.getCiphertext()).isNotEqualTo(plaintext);
    assertThat(encrypted.getAlgorithm()).isEqualTo(cipher.getAlgorithmName());
    
    // Explicitly verify Auth Tag and Nonce presence
    assertThat(encrypted.getNonce()).hasSize(12);
    assertThat(encrypted.getAuthTag()).hasSize(16);

    // Then
    byte[] decrypted = cipher.decrypt(encrypted, key);
    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  void decrypt_WithWrongKey_ShouldThrowException() {
    // Given
    byte[] key1 = new byte[32];
    byte[] key2 = new byte[32];
    random.nextBytes(key1);
    random.nextBytes(key2);
    byte[] plaintext = "data".getBytes();

    // When
    EncryptedData encrypted = cipher.encrypt(plaintext, key1);

    // Then
    assertThatThrownBy(() -> cipher.decrypt(encrypted, key2))
        .isInstanceOf(CryptoOperationException.class);
  }

  @Test
  void decrypt_WithTamperedCiphertext_ShouldThrowException() {
    // Given
    byte[] key = new byte[32];
    random.nextBytes(key);
    byte[] plaintext = "data".getBytes();

    // When
    EncryptedData encrypted = cipher.encrypt(plaintext, key);
    encrypted.getCiphertext()[0] ^= 1; // Tamper with the first byte

    // Then
    assertThatThrownBy(() -> cipher.decrypt(encrypted, key))
        .isInstanceOf(CryptoOperationException.class);
  }

  @Test
  void decrypt_WithTamperedNonce_ShouldThrowException() {
    // Given
    byte[] key = new byte[32];
    random.nextBytes(key);
    byte[] plaintext = "data".getBytes();

    // When
    EncryptedData encrypted = cipher.encrypt(plaintext, key);
    encrypted.getNonce()[0] ^= 1; // Tamper with the first byte

    // Then
    assertThatThrownBy(() -> cipher.decrypt(encrypted, key))
        .isInstanceOf(CryptoOperationException.class);
  }

  @Test
  void decrypt_WithTamperedAuthTag_ShouldThrowException() {
    // Given
    byte[] key = new byte[32];
    random.nextBytes(key);
    byte[] plaintext = "data".getBytes();

    // When
    EncryptedData encrypted = cipher.encrypt(plaintext, key);
    encrypted.getAuthTag()[0] ^= 1; // Tamper with the authentication tag

    // Then
    assertThatThrownBy(() -> cipher.decrypt(encrypted, key))
        .isInstanceOf(CryptoOperationException.class);
  }
}
