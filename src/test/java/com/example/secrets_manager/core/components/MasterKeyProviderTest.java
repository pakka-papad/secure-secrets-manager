package com.example.secrets_manager.core.components;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.core.services.InternalMasterKeyService;
import com.example.secrets_manager.crypto.CryptographyService;
import com.example.secrets_manager.tracing.CorrelationContext;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MasterKeyProviderTest {

  @Mock private CryptographyService cryptographyService;
  @Mock private InternalMasterKeyService internalMasterKeyService;
  private MockEnvironment environment;

  private MasterKeyProvider masterKeyProvider;

  @BeforeEach
  void setUp() {
    environment = new MockEnvironment();
    masterKeyProvider =
        new MasterKeyProvider(cryptographyService, internalMasterKeyService, environment);
    ReflectionTestUtils.setField(masterKeyProvider, "defaultAlgorithm", "AES-256-GCM");
    CorrelationContext.clear();
  }

  @Test
  void init_shouldLoadKeyFromEnvironment_andPromoteIfNewWithinCorrelationContext() {
    // Given
    byte[] keyBytes = new byte[32];
    String base64Key = Base64.getEncoder().encodeToString(keyBytes);

    // Set the property using the original naming convention
    environment.setProperty("MASTER_KEY__V1", base64Key);

    when(internalMasterKeyService.listMasterKeys(any())).thenReturn(List.of());
    when(internalMasterKeyService.getHighestMasterKeyVersion()).thenReturn(0);
    when(cryptographyService.getRequiredSymmetricKeySizeBytes("AES-256-GCM")).thenReturn(32);

    AtomicReference<UUID> capturedCorrelationId = new AtomicReference<>();
    doAnswer(
            inv -> {
              capturedCorrelationId.set(CorrelationContext.get().orElse(null));
              return null;
            })
        .when(internalMasterKeyService)
        .promoteNewKeyInternal(anyInt(), anyString());

    // When
    masterKeyProvider.init();

    // Then
    assertThat(masterKeyProvider.getActiveVersion()).isEqualTo(1);
    assertThat(masterKeyProvider.getMasterKey(1)).isEqualTo(keyBytes);
    verify(internalMasterKeyService).promoteNewKeyInternal(eq(1), eq("AES-256-GCM"));

    assertThat(capturedCorrelationId.get()).isNotNull();
    assertThat(capturedCorrelationId.get().version()).isEqualTo(7);
  }

  @Test
  void init_shouldThrowException_whenRequiredKeyMissingFromEnv() {
    // Given
    MasterKey existingKey =
        MasterKey.builder()
            .version(1)
            .status(MasterKeyState.ACTIVE)
            .encryptAlgo("AES-256-GCM")
            .build();

    when(internalMasterKeyService.listMasterKeys(any())).thenReturn(List.of(existingKey));
    when(internalMasterKeyService.getHighestMasterKeyVersion()).thenReturn(1);

    // When & Then
    assertThrows(IllegalStateException.class, () -> masterKeyProvider.init());
  }

  @Test
  void init_shouldThrowException_whenNoKeysFound() {
    // Given
    when(internalMasterKeyService.listMasterKeys(any())).thenReturn(List.of());
    when(internalMasterKeyService.getHighestMasterKeyVersion()).thenReturn(0);

    // When & Then
    assertThrows(IllegalStateException.class, () -> masterKeyProvider.init());
  }
}
