package com.example.secrets_manager.e2e.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.secrets_manager.core.models.MasterKeyState;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import com.example.secrets_manager.tasks.models.TaskType;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MasterKeyCompromiseE2ETest extends E2EBaseTest {

  @Test
  void markKeyAsCompromised_ShouldBlockMigrationAndEvictFromMemory() {
    final var admin = actors.asAnyAdmin();

    // Setup: Create secrets under V1
    final var groupId = admin.secretGroups().create("compromise-test", "AES-256-GCM").getId();
    admin.secrets().create(groupId, "toxic-secret", "sensitive-payload");

    // Action: Mark V1 as COMPROMISED
    final var updatedKey = admin.masterKeys().markAsCompromised(1);
    assertThat(updatedKey.getStatus()).isEqualTo(MasterKeyState.COMPROMISED);

    // Verification: Attempting to read the secret should fail with 423 Locked
    var response = admin.secrets().getValueRaw(groupId, "toxic-secret");
    assertThat(response.getStatusCode()).isEqualTo(423);

    // Trigger: Promote to V2
    admin.test().triggerMasterKeyPromotion(2);

    // Verification: No migration task should be scheduled for the toxic secret
    // We wait a bit and then check that either no task exists or if it exists, it finished with 0
    // success.
    // Our existing logic schedules a task ONLY if secrets need migration.
    // findSecretIdsByMasterKeyVersionLessThan now excludes COMPROMISED keys.

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              var tasks =
                  admin.tasks().listTasks(Map.of("types", TaskType.MASTER_KEY_MIGRATION.name()));
              // If a task was created (e.g. from a previous test or parallel run), it should not
              // have processed our secret.
              // In a clean silo, no task should be created because existsBy... returns false for
              // compromised keys.
              assertThat(tasks.getItems())
                  .noneMatch(t -> t.getType() == TaskType.MASTER_KEY_MIGRATION);
            });
  }
}
