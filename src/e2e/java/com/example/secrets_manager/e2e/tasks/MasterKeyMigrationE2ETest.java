package com.example.secrets_manager.e2e.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.secrets_manager.e2e.base.E2EBaseTest;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MasterKeyMigrationE2ETest extends E2EBaseTest {

  @Test
  void fullMigrationLifecycle_ShouldPreserveTraceability() {
    final var admin = actors.asAnyAdmin();

    // 1. Setup: Create some secrets under V1
    final var groupId = admin.secretGroups().create("migration-test", "AES-256-GCM").getId();
    for (int i = 1; i <= 5; i++) {
      admin.secrets().create(groupId, "secret-" + i, "value-" + i);
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

    // 5. Final integrity Check
    final var finalTask = admin.tasks().getTask(taskId);
    assertThat(finalTask.getOutput()).isNotNull();
  }
}
