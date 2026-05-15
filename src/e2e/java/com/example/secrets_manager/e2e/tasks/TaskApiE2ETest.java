package com.example.secrets_manager.e2e.tasks;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.api.rest.dto.PagedResponse;
import com.example.secrets_manager.api.rest.dto.TaskSummaryResponse;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskApiE2ETest extends E2EBaseTest {

  @Test
  void taskApi_ShouldEnforceAdministrativeSecurity() {
    final var admin = actors.asAnyAdmin();
    // Use an existing user or create one manually to avoid ActorFactory dependency issues
    final var userId = "user-" + UUID.randomUUID();
    final var password = "Password1234!";
    admin.users().create(userId, password);
    final var user = actors.asUser(userId, password);

    // 1. Admin can list tasks
    var adminResponse = admin.tasks().listTasksRaw(Map.of());
    assertThat(adminResponse.getStatusCode()).isEqualTo(200);

    // 2. Regular user is forbidden
    var userResponse = user.tasks().listTasksRaw(Map.of());
    assertThat(userResponse.getStatusCode()).isEqualTo(403);
  }

  @Test
  void listTasks_ShouldSupportPagination() {
    final var admin = actors.asAnyAdmin();

    // Ensure work exists so tasks are actually created
    final var groupId = admin.secretGroups().create("pagination-test", "AES-256-GCM").getId();
    admin.secrets().create(groupId, "trigger-secret", "value");

    // Trigger system events to ensure tasks exist
    admin.test().triggerMasterKeyPromotion(3);
    admin.test().triggerMasterKeyPromotion(4);

    // Fetch with size 1
    PagedResponse<TaskSummaryResponse> response = admin.tasks().listTasks(Map.of("size", 1));

    assertThat(response.getItems()).hasSize(1);
    assertThat(response.getTotalElements()).isGreaterThanOrEqualTo(2);
  }
}
