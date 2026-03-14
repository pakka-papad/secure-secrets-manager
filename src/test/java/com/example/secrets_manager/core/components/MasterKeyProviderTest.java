package com.example.secrets_manager.core.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.core.services.InternalMasterKeyService;
import com.example.secrets_manager.crypto.CryptographyService;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MasterKeyProviderTest {

  @Mock private CryptographyService cryptographyService;
  @Mock private InternalMasterKeyService internalMasterKeyService;
  @Mock private EnvironmentProvider environmentProvider;

  @InjectMocks private MasterKeyProvider masterKeyProvider;

  private final String testKeyBase64 = Base64.getEncoder().encodeToString(new byte[32]);
  private final String defaultAlgo = "AES-256-GCM";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(masterKeyProvider, "defaultAlgorithm", defaultAlgo);
  }

  @Test
  void init_ShouldLoadActiveAndRetiredKeys() {
    // Given
    var v1 =
        MasterKey.builder()
            .version(1)
            .status(MasterKeyState.RETIRED)
            .encryptAlgo(defaultAlgo)
            .build();
    var v2 =
        MasterKey.builder()
            .version(2)
            .status(MasterKeyState.ACTIVE)
            .encryptAlgo(defaultAlgo)
            .build();

    when(internalMasterKeyService.listMasterKeys(any())).thenReturn(List.of(v1, v2));
    when(internalMasterKeyService.getHighestMasterKeyVersion()).thenReturn(2);
    when(cryptographyService.getRequiredSymmetricKeySizeBytes(defaultAlgo)).thenReturn(32);

    when(environmentProvider.getEnvironment())
        .thenReturn(
            Map.of(
                "MASTER_KEY__V1", testKeyBase64,
                "MASTER_KEY__V2", testKeyBase64));

    // When
    masterKeyProvider.init();

    // Then
    assertThat(masterKeyProvider.getActiveVersion()).isEqualTo(2);
    assertThat(masterKeyProvider.getMasterKey(1)).hasSize(32);
    assertThat(masterKeyProvider.getMasterKey(2)).hasSize(32);
  }

  @Test
  void init_ShouldPromoteNewKey_WhenHighestVersionFound() {
    // Given
    var v1 =
        MasterKey.builder()
            .version(1)
            .status(MasterKeyState.ACTIVE)
            .encryptAlgo(defaultAlgo)
            .build();

    when(internalMasterKeyService.listMasterKeys(any())).thenReturn(List.of(v1));
    when(internalMasterKeyService.getHighestMasterKeyVersion()).thenReturn(1);
    when(cryptographyService.getRequiredSymmetricKeySizeBytes(defaultAlgo)).thenReturn(32);

    when(environmentProvider.getEnvironment())
        .thenReturn(
            Map.of(
                "MASTER_KEY__V1", testKeyBase64,
                "MASTER_KEY__V2", testKeyBase64 // NEW!
                ));

    // When
    masterKeyProvider.init();

    // Then
    verify(internalMasterKeyService).promoteNewKeyInternal(eq(2), eq(defaultAlgo));
    assertThat(masterKeyProvider.getActiveVersion()).isEqualTo(2);
  }

  @Test
  void init_WithMultipleNewKeys_ShouldOnlyPromoteHighest() {
    // Given
    var v1 =
        MasterKey.builder()
            .version(1)
            .status(MasterKeyState.ACTIVE)
            .encryptAlgo(defaultAlgo)
            .build();

    when(internalMasterKeyService.listMasterKeys(any())).thenReturn(List.of(v1));
    when(internalMasterKeyService.getHighestMasterKeyVersion()).thenReturn(1);
    when(cryptographyService.getRequiredSymmetricKeySizeBytes(defaultAlgo)).thenReturn(32);

    when(environmentProvider.getEnvironment())
        .thenReturn(
            Map.of(
                "MASTER_KEY__V1", testKeyBase64,
                "MASTER_KEY__V2", testKeyBase64,
                "MASTER_KEY__V3", testKeyBase64 // HIGHEST
                ));

    // When
    masterKeyProvider.init();

    // Then
    // Only V3 should be promoted
    verify(internalMasterKeyService, times(1)).promoteNewKeyInternal(eq(3), eq(defaultAlgo));
    verify(internalMasterKeyService, never()).promoteNewKeyInternal(eq(2), any());

    // Memory should contain V1 (existing) and V3 (new active), but NOT V2 (ignored)
    assertThat(masterKeyProvider.getMasterKey(1)).isNotNull();
    assertThat(masterKeyProvider.getMasterKey(3)).isNotNull();
    assertThatThrownBy(() -> masterKeyProvider.getMasterKey(2))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void init_ShouldFail_WhenRequiredKeyIsMissingFromEnv() {
    // Given
    var v1 =
        MasterKey.builder()
            .version(1)
            .status(MasterKeyState.ACTIVE)
            .encryptAlgo(defaultAlgo)
            .build();

    when(internalMasterKeyService.listMasterKeys(any())).thenReturn(List.of(v1));
    when(environmentProvider.getEnvironment()).thenReturn(Collections.emptyMap());

    // When & Then
    assertThatThrownBy(() -> masterKeyProvider.init())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Required Master Key v1 is missing from ENV");
  }

  @Test
  void init_ShouldExcludeCompromisedKeysFromMemory() {
    // Given
    var v1 =
        MasterKey.builder()
            .version(1)
            .status(MasterKeyState.COMPROMISED)
            .encryptAlgo(defaultAlgo)
            .build();
    var v2 =
        MasterKey.builder()
            .version(2)
            .status(MasterKeyState.ACTIVE)
            .encryptAlgo(defaultAlgo)
            .build();

    // requiredKeys list only contains ACTIVE/RETIRED
    when(internalMasterKeyService.listMasterKeys(any())).thenReturn(List.of(v2));
    when(internalMasterKeyService.getHighestMasterKeyVersion()).thenReturn(2);
    when(cryptographyService.getRequiredSymmetricKeySizeBytes(defaultAlgo)).thenReturn(32);

    when(environmentProvider.getEnvironment())
        .thenReturn(
            Map.of(
                "MASTER_KEY__V1", testKeyBase64, // Toxic!
                "MASTER_KEY__V2", testKeyBase64));

    // When
    masterKeyProvider.init();

    // Then
    assertThat(masterKeyProvider.getActiveVersion()).isEqualTo(2);
    assertThatThrownBy(() -> masterKeyProvider.getMasterKey(1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not available in memory");
  }

  @Test
  void getMasterKey_WithNonExistentVersion_ShouldThrowException() {
    // Given
    when(internalMasterKeyService.listMasterKeys(any())).thenReturn(Collections.emptyList());
    when(internalMasterKeyService.getHighestMasterKeyVersion()).thenReturn(0);
    when(cryptographyService.getRequiredSymmetricKeySizeBytes(any())).thenReturn(32);
    when(environmentProvider.getEnvironment()).thenReturn(Map.of("MASTER_KEY__V1", testKeyBase64));

    masterKeyProvider.init();

    // When & Then
    assertThatThrownBy(() -> masterKeyProvider.getMasterKey(99))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
