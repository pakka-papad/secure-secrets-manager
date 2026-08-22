package com.example.secrets_manager.e2e.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.secrets_manager.core.models.MasterKey;
import com.example.secrets_manager.e2e.actor.E2EActor;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationInput;
import com.example.secrets_manager.tasks.models.masterkeymigration.MasterKeyMigrationOutput;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MasterKeyMigrationE2ETest extends E2EBaseTest {

  @Test
  void discoverNewMasterKey_ShouldCreateTaskAndMigrateExistingSecrets() {
    final var admin = actors.asAnyAdmin();

    // 1. Setup: Create secrets under the current active key
    final var groupId = admin.secretGroups().create("migration-test", "AES-256-GCM").getId();
    Map<String, String> expectedSecrets = new HashMap<>();
    for (int i = 1; i <= 5; i++) {
      String name = "secret-" + i;
      String value = "value-" + i;
      admin.secrets().create(groupId, name, value);
      expectedSecrets.put(name, value);
    }

    final int targetVersion = nextMasterKeyVersion(admin);
    final var existingTaskIds = migrationTaskIds(admin);

    // 2. Trigger: Add the next Master Key to the environment and run provider discovery
    admin.test().triggerMasterKeyPromotion(targetVersion);

    // 3. Verification: Find the migration task
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              assertThat(migrationTaskIds(admin))
                  .anyMatch(taskId -> !existingTaskIds.contains(taskId));
            });

    final var taskSummary =
        admin
            .tasks()
            .listTasks(Map.of("types", TaskType.MASTER_KEY_MIGRATION.name()))
            .getItems()
            .stream()
            .filter(task -> !existingTaskIds.contains(task.getId()))
            .findFirst()
            .orElseThrow();
    final var taskId = taskSummary.getId();

    // 4. Wait for completion
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              final var task = admin.tasks().getTask(taskId);
              assertThat(task.getState()).isEqualTo(TaskState.COMPLETED);
            });

    // 5. Verify the task migrated every secret and preserved traceability
    final var finalTask = admin.tasks().getTask(taskId);
    assertThat(finalTask.getCorrelationId()).isNotNull();
    assertThat(finalTask.getInput()).isInstanceOf(MasterKeyMigrationInput.class);
    assertThat(((MasterKeyMigrationInput) finalTask.getInput()).getTargetMasterKeyVersion())
        .isEqualTo(targetVersion);
    assertThat(finalTask.getOutput()).isInstanceOf(MasterKeyMigrationOutput.class);
    final var output = (MasterKeyMigrationOutput) finalTask.getOutput();
    assertThat(output.getSuccessfullyMigrated()).isGreaterThanOrEqualTo(expectedSecrets.size());
    assertThat(output.getTotalFailures()).isZero();
    assertThat(output.getErrorDetails()).isEmpty();

    // 6. Final integrity check: read back secrets and verify they can still be decrypted
    expectedSecrets.forEach(
        (name, expectedValue) -> {
          var response = admin.secrets().getValue(groupId, name);
          assertThat(response.getPlaintextValue())
              .as("Secret '%s' decryption check", name)
              .isEqualTo(expectedValue);
        });
  }

  @Test
  void cancelPendingMigration_ShouldPreventExecution() {
    final var admin = actors.asAnyAdmin();

    // 1. Setup: Create a secret under the current active key
    final var groupId = admin.secretGroups().create("cancel-test", "AES-256-GCM").getId();
    admin.secrets().create(groupId, "target-secret", "sensitive-value");

    final int targetVersion = nextMasterKeyVersion(admin);
    final var existingTaskIds = migrationTaskIds(admin);

    // 2. Trigger: Discover the next Master Key
    admin.test().triggerMasterKeyPromotion(targetVersion);

    // 3. Discovery: Find the PENDING task
    final var tasks =
        await()
            .atMost(Duration.ofSeconds(5))
            .until(
                () ->
                    admin
                        .tasks()
                        .listTasks(
                            Map.of(
                                "types",
                                TaskType.MASTER_KEY_MIGRATION.name(),
                                "states",
                                TaskState.PENDING.name()))
                        .getItems()
                        .stream()
                        .filter(task -> !existingTaskIds.contains(task.getId()))
                        .toList(),
                items -> !items.isEmpty());

    final var taskId = tasks.get(0).getId();

    // 4. Action: Cancel immediately
    final var cancelledTask = admin.tasks().cancelTask(taskId);

    // 5. Assert: Task state must be CANCELLED
    assertThat(cancelledTask.getState()).isEqualTo(TaskState.CANCELLED);
  }

  private int nextMasterKeyVersion(E2EActor admin) {
    return admin.masterKeys().list(Map.of()).getItems().stream()
            .map(MasterKey::getVersion)
            .max(Comparator.naturalOrder())
            .orElse(0)
        + 1;
  }

  private Set<UUID> migrationTaskIds(E2EActor admin) {
    return admin
        .tasks()
        .listTasks(Map.of("types", TaskType.MASTER_KEY_MIGRATION.name()))
        .getItems()
        .stream()
        .map(task -> task.getId())
        .collect(Collectors.toSet());
  }
}
