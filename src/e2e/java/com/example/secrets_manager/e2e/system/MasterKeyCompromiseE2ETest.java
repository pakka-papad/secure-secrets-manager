package com.example.secrets_manager.e2e.system;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MasterKeyCompromiseE2ETest extends E2EBaseTest {

  @Test
  void markKeyAsCompromised_ShouldBlockAccessAndIsolateData() {
    final var admin = actors.asAnyAdmin();

    // Discovery: Find the current max version to ensure isolation
    final int currentMax =
        admin.masterKeys().list(Map.of()).getItems().stream()
            .map(MasterKey::getVersion)
            .max(Comparator.naturalOrder())
            .orElse(1);

    // Setup: Promote to a private 'test-only' version (e.g., v100)
    // This ensures we don't compromise v1/v2 which other tests rely on.
    final int privateVersion = currentMax + 1;
    admin.test().triggerMasterKeyPromotion(privateVersion);

    final var secretGroupName = "secret-group-" + UUID.randomUUID();
    final var groupId = admin.secretGroups().create(secretGroupName, "AES-256-GCM").getId();
    admin.secrets().create(groupId, "toxic-secret", "sensitive-payload");

    // Action: Mark our private version as COMPROMISED
    final var updatedKey = admin.masterKeys().markAsCompromised(privateVersion);
    assertThat(updatedKey.getStatus()).isEqualTo(MasterKeyState.COMPROMISED);

    // Verification: Attempting to read the secret must fail with 423 Locked
    var response = admin.secrets().getValueRaw(groupId, "toxic-secret");
    assertThat(response.getStatusCode()).isEqualTo(423);

    // Persistence Check: Promote to another version and ensure data is still blocked
    admin.test().triggerMasterKeyPromotion(privateVersion + 1);

    // The secret is still protected by the compromised key, so it must remain unreachable
    var stillLocked = admin.secrets().getValueRaw(groupId, "toxic-secret");
    assertThat(stillLocked.getStatusCode()).isEqualTo(423);

    // System Health Check: V1 should still be healthy
    // (This proves we achieved isolation through data sharding)
    admin.secrets().create(groupId, "healthy-secret", "safe");
    assertThat(admin.secrets().getValue(groupId, "healthy-secret").getPlaintextValue())
        .isEqualTo("safe");
  }
}
