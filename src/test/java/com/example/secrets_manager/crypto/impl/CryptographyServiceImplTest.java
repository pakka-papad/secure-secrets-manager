package com.example.secrets_manager.crypto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.crypto.CipherPurpose;
import com.example.secrets_manager.crypto.PasswordHasher;
import com.example.secrets_manager.crypto.SymmetricCipher;
import com.example.secrets_manager.crypto.dto.BinaryHash;
import com.example.secrets_manager.crypto.dto.EncryptedData;
import com.example.secrets_manager.crypto.dto.HashedPassword;
import com.example.secrets_manager.crypto.dto.SymmetricAlgorithmMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CryptographyServiceImplTest {

  @Mock private PasswordHasher mockHasher;
  @Mock private SymmetricCipher mockDataCipher;
  @Mock private SymmetricCipher mockKwOnlyCipher;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private CryptographyServiceImpl cryptographyService;

  @BeforeEach
  void setUp() {
    lenient().when(mockHasher.getAlgorithmName()).thenReturn("BCRYPT");

    // Mock a general-purpose cipher
    lenient().when(mockDataCipher.getAlgorithmName()).thenReturn("AES_GCM_256");
    lenient().when(mockDataCipher.getRequiredKeySizeBytes()).thenReturn(32);
    lenient()
        .when(mockDataCipher.getSupportedPurposes())
        .thenReturn(Set.of(CipherPurpose.DATA, CipherPurpose.KEY_WRAP));

    // Mock a key-wrap only cipher
    lenient().when(mockKwOnlyCipher.getAlgorithmName()).thenReturn("AES_KW_256");
    lenient().when(mockKwOnlyCipher.getRequiredKeySizeBytes()).thenReturn(32);
    lenient()
        .when(mockKwOnlyCipher.getSupportedPurposes())
        .thenReturn(Set.of(CipherPurpose.KEY_WRAP));

    cryptographyService =
        new CryptographyServiceImpl(
            List.of(mockHasher), List.of(mockDataCipher, mockKwOnlyCipher), objectMapper);
  }

  @Test
  void getSupportedAlgorithms_ShouldFilterByPurpose() {
    // 1. DATA purpose should only return the data cipher
    List<SymmetricAlgorithmMetadata> dataAlgos =
        cryptographyService.getSupportedAlgorithms(CipherPurpose.DATA);
    assertThat(dataAlgos).hasSize(1);
    assertThat(dataAlgos.get(0).name()).isEqualTo("AES_GCM_256");
    assertThat(dataAlgos.get(0).supportedPurposes())
        .containsExactlyInAnyOrder(CipherPurpose.DATA, CipherPurpose.KEY_WRAP);

    // 2. KEY_WRAP purpose should return both
    List<SymmetricAlgorithmMetadata> kwAlgos =
        cryptographyService.getSupportedAlgorithms(CipherPurpose.KEY_WRAP);
    assertThat(kwAlgos).hasSize(2);
    assertThat(kwAlgos)
        .extracting(SymmetricAlgorithmMetadata::name)
        .containsExactlyInAnyOrder("AES_GCM_256", "AES_KW_256");

    // Verify purpose field in KW only metadata
    SymmetricAlgorithmMetadata kwOnly =
        kwAlgos.stream().filter(a -> a.name().equals("AES_KW_256")).findFirst().get();
    assertThat(kwOnly.supportedPurposes()).containsExactly(CipherPurpose.KEY_WRAP);

    // 3. getSupportedAlgorithms() without purpose should return ALL algorithms
    List<SymmetricAlgorithmMetadata> allAlgos = cryptographyService.getSupportedAlgorithms();
    assertThat(allAlgos).hasSize(2);
    assertThat(allAlgos)
        .extracting(SymmetricAlgorithmMetadata::name)
        .containsExactlyInAnyOrder("AES_GCM_256", "AES_KW_256");
  }

  @Test
  void isAlgorithmSupported_ShouldCheckPurpose() {
    // Overloaded: purpose check
    assertThat(cryptographyService.isAlgorithmSupported("AES_GCM_256", CipherPurpose.DATA))
        .isTrue();
    assertThat(cryptographyService.isAlgorithmSupported("AES_GCM_256", CipherPurpose.KEY_WRAP))
        .isTrue();

    assertThat(cryptographyService.isAlgorithmSupported("AES_KW_256", CipherPurpose.DATA))
        .isFalse();
    assertThat(cryptographyService.isAlgorithmSupported("AES_KW_256", CipherPurpose.KEY_WRAP))
        .isTrue();

    // Overloaded: existence check
    assertThat(cryptographyService.isAlgorithmSupported("AES_GCM_256")).isTrue();
    assertThat(cryptographyService.isAlgorithmSupported("AES_KW_256")).isTrue();
    assertThat(cryptographyService.isAlgorithmSupported("NON_EXISTENT")).isFalse();
  }

  @Test
  void generateKey_ShouldReturnCorrectKeySpec() {
    // Given
    String algo = "AES_GCM_256";

    // When
    SecretKey key = cryptographyService.generateKey(algo);

    // Then
    assertThat(key).isNotNull();
    assertThat(key.getAlgorithm()).isEqualTo("RAW");
    assertThat(key.getEncoded()).hasSize(32);
  }

  @Test
  void generateKey_WithUnsupportedAlgorithm_ShouldThrowException() {
    // When & Then
    assertThatThrownBy(() -> cryptographyService.generateKey("UNSUPPORTED"))
        .isInstanceOf(IllegalArgumentException.class);
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
    when(mockDataCipher.encrypt(data, key)).thenReturn(expected);

    // When
    EncryptedData result = cryptographyService.encrypt(data, key, "AES_GCM_256");

    // Then
    assertThat(result).isEqualTo(expected);
    verify(mockDataCipher).encrypt(data, key);
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
