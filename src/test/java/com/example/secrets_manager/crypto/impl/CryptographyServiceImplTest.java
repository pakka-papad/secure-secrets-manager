package com.example.secrets_manager.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.secrets_manager.crypto.CipherPurpose;
import com.example.secrets_manager.crypto.PasswordHasher;
import com.example.secrets_manager.crypto.dto.BinaryHash;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.example.secrets_manager.crypto.dto.SymmetricAlgorithmMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CryptographyServiceImplTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private CryptographyServiceImpl cryptographyService;

  @BeforeEach
  void setUp() {
    final var hashers = List.<PasswordHasher>of(new BcryptPasswordHasher());

    final var ciphers =
        List.of(
            new AesGcmSymmetricCipher(),
            new ChaCha20Poly1305SymmetricCipher(),
            new AesKw128SymmetricCipher(),
            new AesKw192SymmetricCipher(),
            new AesKw256SymmetricCipher());

    cryptographyService = new CryptographyServiceImpl(hashers, ciphers, objectMapper);
  }

  @Test
  void getSupportedAlgorithms_ShouldFilterByPurpose() {
    // 1. DATA purpose should return GCM and ChaCha
    List<SymmetricAlgorithmMetadata> dataAlgos =
        cryptographyService.getSupportedAlgorithms(CipherPurpose.DATA);
    assertThat(dataAlgos)
        .extracting(SymmetricAlgorithmMetadata::name)
        .containsExactlyInAnyOrder("AES-256-GCM", "CHACHA20-POLY1305");

    // 2. KEY_WRAP purpose should return all 5
    List<SymmetricAlgorithmMetadata> kwAlgos =
        cryptographyService.getSupportedAlgorithms(CipherPurpose.KEY_WRAP);
    assertThat(kwAlgos).hasSize(5);

    // 3. getSupportedAlgorithms() without purpose should return ALL
    assertThat(cryptographyService.getSupportedAlgorithms()).hasSize(5);
  }

  @Test
  void isAlgorithmSupported_ShouldCheckPurpose() {
    assertThat(cryptographyService.isAlgorithmSupported("AES-256-GCM", CipherPurpose.DATA))
        .isTrue();
    assertThat(cryptographyService.isAlgorithmSupported("AES-KW-256", CipherPurpose.DATA))
        .isFalse();
    assertThat(cryptographyService.isAlgorithmSupported("CHACHA20-POLY1305")).isTrue();
  }

  @ParameterizedTest(name = "Algorithm: {0}, Size: {1}")
  @CsvSource({
    "AES-256-GCM, 32",
    "CHACHA20-POLY1305, 32",
    "AES-KW-128, 16",
    "AES-KW-192, 24",
    "AES-KW-256, 32"
  })
  void generateKey_ShouldReturnCorrectKeySpec(String algo, int expectedSize) {
    // When
    SecretKey key = cryptographyService.generateKey(algo);

    // Then
    assertThat(key).isNotNull();
    assertThat(key.getEncoded()).hasSize(expectedSize);
  }

  @Test
  void generateKey_WithUnsupportedAlgorithm_ShouldThrowException() {
    // When & Then
    assertThatThrownBy(() -> cryptographyService.generateKey("UNSUPPORTED"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void hashPassword_ShouldProduceValidHash() {
    // Given
    byte[] pass = "password123".getBytes();

    // When
    HashedPassword result = cryptographyService.hashPassword(pass);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getAlgorithm()).isEqualTo("BCRYPT");
    assertThat(result.getDigest()).isNotEmpty();

    // Verify it actually works
    assertThat(cryptographyService.verifyPassword(pass, result)).isTrue();
    assertThat(cryptographyService.verifyPassword("wrong".getBytes(), result)).isFalse();
  }

  @Test
  void encrypt_ShouldDelegateToCipher() {
    // Given
    byte[] data = "data".getBytes();
    byte[] key = new byte[32];
    String algo = "AES-256-GCM";

    // When
    EncryptedData result = cryptographyService.encrypt(data, key, algo);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getAlgorithm()).isEqualTo(algo);
    assertThat(result.getCiphertext()).isNotEmpty();
  }

  @Test
  void createDataHash_ShouldBeDeterministic() {
    // Given
    String data = "some data to hash";

    // When
    byte[] hash1 = cryptographyService.createDataHash(data);
    byte[] hash2 = cryptographyService.createDataHash(data);

    // Then
    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).hasSize(32); // SHA-256
  }

  @Test
  void hashBytes_ShouldProduceValidHash() {
    // Given
    byte[] data = "bytes".getBytes();

    // When
    BinaryHash result = cryptographyService.hashBytes(data);

    // Then
    assertThat(result.getAlgorithm()).isEqualTo("SHA-256");
    assertThat(result.getHash()).hasSize(32);
  }
}
