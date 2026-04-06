package com.example.secrets_manager.e2e.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.e2e.base.E2EBaseTest;
import org.junit.jupiter.api.Test;

class SystemMetadataE2ETest extends E2EBaseTest {

  @Test
  void shouldDiscoverSupportedAlgorithms() {
    var admin = actors.asAnyAdmin();

    var algorithms = admin.systemMetadata().getSymmetricAlgorithms();

    assertThat(algorithms).isNotEmpty();
    assertThat(algorithms).anyMatch(a -> a.name().equals("AES-256-GCM") && a.keySizeBytes() == 32);
    assertThat(algorithms)
        .anyMatch(a -> a.name().equals("CHACHA20-POLY1305") && a.keySizeBytes() == 32);
  }
}
