package com.example.secrets_manager.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.secrets_manager.crypto.exception.CryptoOperationException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChaCha20Poly1305SymmetricCipherTest {

  private ChaCha20Poly1305SymmetricCipher cipher;
  private byte[] validKey;
  private final SecureRandom secureRandom = new SecureRandom();

  @BeforeEach
  void setUp() {
    cipher = new ChaCha20Poly1305SymmetricCipher();
    validKey = new byte[32];
    secureRandom.nextBytes(validKey);
  }

  @Test
  @DisplayName("Encryption and decryption should be successful for valid inputs")
  void encryptDecrypt_ShouldBeSuccessful() throws CryptoOperationException {
    byte[] plaintext = "Hello ChaCha20!".getBytes(StandardCharsets.UTF_8);

    var encrypted = cipher.encrypt(plaintext, validKey);
    
    assertThat(encrypted.getAlgorithm()).isEqualTo("CHACHA20-POLY1305");
    assertThat(encrypted.getCiphertext()).isNotEqualTo(plaintext);
    assertThat(encrypted.getNonce()).hasSize(12);
    assertThat(encrypted.getAuthTag()).hasSize(16);

    byte[] decrypted = cipher.decrypt(encrypted, validKey);
    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  @DisplayName("Decryption should fail if the authentication tag is tampered with")
  void decrypt_ShouldFailIfTagTampered() {
    byte[] plaintext = "Sensitive data".getBytes(StandardCharsets.UTF_8);
    var encrypted = cipher.encrypt(plaintext, validKey);

    // Tamper with the authentication tag
    encrypted.getAuthTag()[0] ^= (byte) 0xFF;

    assertThatThrownBy(() -> cipher.decrypt(encrypted, validKey))
        .isInstanceOf(CryptoOperationException.class)
        .hasMessageContaining("Data may be corrupt");
  }

  @Test
  @DisplayName("Decryption should fail if the wrong key is used")
  void decrypt_ShouldFailWithWrongKey() {
    byte[] plaintext = "Secret message".getBytes(StandardCharsets.UTF_8);
    var encrypted = cipher.encrypt(plaintext, validKey);

    byte[] wrongKey = new byte[32];
    secureRandom.nextBytes(wrongKey);

    assertThatThrownBy(() -> cipher.decrypt(encrypted, wrongKey))
        .isInstanceOf(CryptoOperationException.class);
  }

  @Test
  @DisplayName("Encryption should throw exception for invalid key size")
  void encrypt_ShouldFailForInvalidKeySize() {
    byte[] plaintext = "Data".getBytes(StandardCharsets.UTF_8);
    byte[] shortKey = new byte[16];

    assertThatThrownBy(() -> cipher.encrypt(plaintext, shortKey))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be 32 bytes");
  }
}
