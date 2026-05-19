package com.example.secrets_manager.e2e.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.secrets_manager.e2e.base.E2EBaseTest;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MasterKeyMigrationE2ETest extends E2EBaseTest {

  @Test
  void fullMigrationLifecycle_ShouldPreserveTraceability() {
    final var admin = actors.asAnyAdmin();

    // 1. Setup: Create some secrets under V1
    final var groupId = admin.secretGroups().create("migration-test", "AES-256-GCM").getId();
    Map<String, String> expectedSecrets = new HashMap<>();
    for (int i = 1; i <= 5; i++) {
      String name = "secret-" + i;
      String value = "value-" + i;
      admin.secrets().create(groupId, name, value);
      expectedSecrets.put(name, value);
    }

    // 2. Trigger: Promote to Master Key V2 (via internal test backdoor)
    admin.test().triggerMasterKeyPromotion(2);

    // 3. Verification: Find the migration task
    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              var tasks =
                  admin.tasks().listTasks(Map.of("types", TaskType.MASTER_KEY_MIGRATION.name()));
              assertThat(tasks.getItems()).isNotEmpty();
            });

    final var taskSummary =
        admin
            .tasks()
            .listTasks(Map.of("types", TaskType.MASTER_KEY_MIGRATION.name()))
            .getItems()
            .get(0);
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

    // 5. Final integrity Check: Read back secrets and verify they can still be decrypted
    final var finalTask = admin.tasks().getTask(taskId);
    assertThat(finalTask.getOutput()).isNotNull();

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

    // 1. Setup: Create some secrets under V2
    final var groupId = admin.secretGroups().create("cancel-test", "AES-256-GCM").getId();
    admin.secrets().create(groupId, "target-secret", "sensitive-value");

    // 2. Trigger: Promote to Master Key V3
    admin.test().triggerMasterKeyPromotion(3);

    // 3. Discovery: Find the PENDING task
    // We use a small wait to ensure the event was processed but the poller (1s interval)
    // likely hasn't claimed it yet if we are quick.
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
                        .getItems(),
                items -> !items.isEmpty());

    final var taskId = tasks.get(0).getId();

    // 4. Action: Cancel immediately
    final var cancelledTask = admin.tasks().cancelTask(taskId);

    // 5. Assert: Task state must be CANCELLED and it must never have started
    assertThat(cancelledTask.getState()).isEqualTo(TaskState.CANCELLED);
    assertThat(cancelledTask.getStartedAt()).isNull();
  }
}
