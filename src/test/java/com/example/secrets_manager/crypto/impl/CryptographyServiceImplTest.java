package com.example.secrets_manager.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.crypto.PasswordHasher;
import com.example.secrets_manager.crypto.SymmetricCipher;
import com.example.secrets_manager.crypto.dto.BinaryHash;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CryptographyServiceImplTest {

  @Mock private PasswordHasher mockHasher;
  @Mock private SymmetricCipher mockCipher;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private CryptographyServiceImpl cryptographyService;

  @BeforeEach
  void setUp() {
    when(mockHasher.getAlgorithmName()).thenReturn("BCRYPT");
    when(mockCipher.getAlgorithmName()).thenReturn("AES_GCM_256");

    cryptographyService =
        new CryptographyServiceImpl(List.of(mockHasher), List.of(mockCipher), objectMapper);
  }

  @Test
  void hashPassword_ShouldDelegateToHasher() {
    // Given
    byte[] pass = "pass".getBytes();
    HashedPassword expected =
        new HashedPassword(new byte[] {1}, new byte[] {2}, "BCRYPT", Map.of());
    when(mockHasher.hash(pass)).thenReturn(expected);

    // When
    HashedPassword result = cryptographyService.hashPassword(pass);

    // Then
    assertThat(result).isEqualTo(expected);
    verify(mockHasher).hash(pass);
  }

  @Test
  void encrypt_ShouldDelegateToCipher() {
    // Given
    byte[] data = "data".getBytes();
    byte[] key = new byte[32];
    EncryptedData expected =
        new EncryptedData(new byte[] {1}, new byte[] {2}, new byte[] {3}, "AES_GCM_256");
    when(mockCipher.encrypt(data, key)).thenReturn(expected);

    // When
    EncryptedData result = cryptographyService.encrypt(data, key, "AES_GCM_256");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(mockCipher).encrypt(data, key);
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
