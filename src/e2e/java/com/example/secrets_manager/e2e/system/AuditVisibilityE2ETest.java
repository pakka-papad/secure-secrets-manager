package com.example.secrets_manager.e2e.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.secrets_manager.api.rest.dto.AuditLogSummaryResponse;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import com.example.secrets_manager.e2e.client.AuthClient;
import com.example.secrets_manager.tasks.models.TaskState;
import com.example.secrets_manager.tasks.models.TaskType;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditVisibilityE2ETest extends E2EBaseTest {

  @Test
  void auditLogTraceability_ShouldLinkActionsViaCorrelationId() {
    final var admin = actors.asAnyAdmin();

    // Scenario A: The "Audit Thread" Forensic Check
    // 1. Action: Admin creates a new Secret Group via client
    final var response =
        admin.secretGroups().createRaw("audit-test-group-" + UUID.randomUUID(), "AES-256-GCM");

    response.then().statusCode(201);

    // 2. Capture: X-Correlation-ID from the REST response header
    final var correlationId = response.getHeader("X-Correlation-ID");
    assertThat(correlationId).isNotNull();

    // 3. Verification: Audit log exists and matches ID
    final var auditLogs = admin.audit().list(Map.of("correlationId", correlationId));
    assertThat(auditLogs.getItems()).hasSize(1);

    final var logEntry = auditLogs.getItems().get(0);
    assertThat(logEntry.getAction().name()).isEqualTo("SECRET_GROUP_CREATE");
    assertThat(logEntry.getCorrelationId()).isEqualTo(UUID.fromString(correlationId));

    // 4. Chain Check: Detailed log verification
    final var detailedLog = admin.audit().get(logEntry.getSeqId());
    assertThat(detailedLog.getDataHash()).isNotNull();
    assertThat(detailedLog.getPrevHash()).isNotNull();
  }

  @Test
  void securityEventLog_ShouldCaptureAuthFailures() {
    final var admin = actors.asAnyAdmin();

    // Scenario B: Security Event Isolation
    // 1. Action: Attempt to login with an invalid password via AuthClient
    final var authClient = new AuthClient();
    authClient
        .loginRaw("non-existent-" + UUID.randomUUID(), "WrongPass123!")
        .then()
        .statusCode(401);

    // 2. Verification: Search for LOGIN_FAILED events
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var events = admin.securityEvents().list(Map.of("action", "LOGIN_FAILED"));
              assertThat(events.getItems()).isNotEmpty();
            });
  }

  @Test
  void forensicApis_ShouldEnforceSecurity() {
    final var admin = actors.asAnyAdmin();

    // Create the user first so we can log in
    final var username = "audit-user-" + UUID.randomUUID();
    final var password = "UserPass123!";
    admin.users().create(username, password);
    final var user = actors.asUser(username, password);

    // Scenario C: Administrative Access Control
    // 1. Admin can access
    assertThat(admin.audit().listRaw(Map.of()).getStatusCode()).isEqualTo(200);
    assertThat(admin.securityEvents().listRaw(Map.of()).getStatusCode()).isEqualTo(200);

    // 2. Regular user is forbidden
    assertThat(user.audit().listRaw(Map.of()).getStatusCode()).isEqualTo(403);
    assertThat(user.securityEvents().listRaw(Map.of()).getStatusCode()).isEqualTo(403);
  }

  @Test
  void migrationTraceability_ShouldPreserveCausalityAcrossThreads() {
    final var admin = actors.asAnyAdmin();

    // Scenario D: Migration Traceability
    // 1. Setup: Create secrets
    final var groupId =
        admin.secretGroups().create("trace-migration-" + UUID.randomUUID(), "AES-256-GCM").getId();
    admin.secrets().create(groupId, "trace-secret", "val");

    // 2. Trigger: Master Key Rotation
    admin.test().triggerMasterKeyPromotion(2);

    // 3. Find and Wait for Task
    await()
        .atMost(Duration.ofSeconds(15))
        .until(
            () ->
                admin
                    .tasks()
                    .listTasks(Map.of("types", TaskType.MASTER_KEY_MIGRATION.name()))
                    .getItems(),
            items -> !items.isEmpty());

    final var taskSummary =
        admin
            .tasks()
            .listTasks(Map.of("types", TaskType.MASTER_KEY_MIGRATION.name()))
            .getItems()
            .get(0);
    final var taskCid = taskSummary.getCorrelationId();

    await()
        .atMost(Duration.ofSeconds(30))
        .until(() -> admin.tasks().getTask(taskSummary.getId()).getState() == TaskState.COMPLETED);

    // 4. Verification: All audit logs share the task Correlation ID
    final var auditLogs = admin.audit().list(Map.of("correlationId", taskCid));

    // Expect: MASTER_KEY_PROMOTED and SECRET_MASTER_KEY_UPGRADED
    assertThat(auditLogs.getItems())
        .extracting(AuditLogSummaryResponse::getAction)
        .extracting(Enum::name)
        .contains("MASTER_KEY_PROMOTED", "SECRET_MASTER_KEY_UPGRADED");
  }
}
