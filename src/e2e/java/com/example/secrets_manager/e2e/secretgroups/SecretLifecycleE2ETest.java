package com.example.secrets_manager.e2e.secretgroups;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.api.rest.dto.CreateSecretRequest;
import com.example.secrets_manager.api.rest.dto.SecretMetadataResponse;
import com.example.secrets_manager.api.rest.dto.UpdateSecretValueRequest;
import com.example.secrets_manager.core.models.PermissionType;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecretLifecycleE2ETest extends E2EBaseTest {

  @Test
  @DisplayName("Secrets: Full CRUD lifecycle with envelope encryption")
  void secretLifecycle_ShouldSucceed() {
    var admin = actors.asAnyAdmin();
    String groupName = "lifecycle-group-" + UUID.randomUUID();
    var group = admin.secretGroups().create(groupName, "AES-256-GCM");
    UUID groupId = group.getId();

    String secretName = "db_password";
    String initialValue = "super-secret-123";

    // 1. Create Secret
    var created = admin.secrets().create(groupId, secretName, initialValue);
    assertThat(created.getName()).isEqualTo(secretName);
    assertThat(created.getId()).isNotNull();

    // 2. List Secrets (Metadata)
    var list = admin.secrets().list(groupId, Map.of());
    assertThat(list.getItems()).anyMatch(s -> s.getName().equals(secretName));

    // 3. Reveal Value
    var valueResponse = admin.secrets().getValue(groupId, secretName);
    assertThat(valueResponse.getPlaintextValue()).isEqualTo(initialValue);

    // 4. Update Value
    String newValue = "new-and-improved-password";
    admin.secrets().updateValue(groupId, secretName, newValue);

    // 5. Verify Updated Value
    var updatedValueResponse = admin.secrets().getValue(groupId, secretName);
    assertThat(updatedValueResponse.getPlaintextValue()).isEqualTo(newValue);

    // 6. Delete Secret
    admin.secrets().delete(groupId, secretName);

    // 7. Verify 404 on Metadata and Value
    admin.secrets().getValueRaw(groupId, secretName).then().statusCode(404);
    admin
        .secrets()
        .list(groupId, Map.of())
        .getItems()
        .forEach(s -> assertThat(s.getName()).isNotEqualTo(secretName));
  }

  @Test
  @DisplayName("Secrets: Should handle duplicate names in same group")
  void createSecret_WithDuplicateName_ShouldReturn409() {
    var admin = actors.asAnyAdmin();
    var group = admin.secretGroups().create("dup-check-" + UUID.randomUUID(), "AES-256-GCM");

    admin.secrets().create(group.getId(), "my-key", "val1");

    var request = new CreateSecretRequest("my-key", "val2");
    admin.secrets().createRaw(group.getId(), request).then().statusCode(409);
  }

  @Test
  @DisplayName("Secrets: Should support prefix filtering")
  void listSecrets_WithPrefix_ShouldFilterResults() {
    var admin = actors.asAnyAdmin();
    var group = admin.secretGroups().create("filter-group-" + UUID.randomUUID(), "AES-256-GCM");
    UUID gid = group.getId();

    admin.secrets().create(gid, "prod_db", "p1");
    admin.secrets().create(gid, "prod_api", "p2");
    admin.secrets().create(gid, "dev_db", "d1");

    // Filter by "prod_"
    var prodList = admin.secrets().list(gid, Map.of("namePrefix", "prod_"));
    assertThat(prodList.getItems())
        .hasSize(2)
        .extracting(SecretMetadataResponse::getName)
        .containsExactlyInAnyOrder("prod_db", "prod_api");

    // Filter by "dev_"
    var devList = admin.secrets().list(gid, Map.of("namePrefix", "dev_"));
    assertThat(devList.getItems())
        .hasSize(1)
        .extracting(SecretMetadataResponse::getName)
        .containsExactly("dev_db");
  }

  @Test
  @DisplayName("Secrets: Governance should restrict access based on ACL")
  void secretGovernance_ShouldRespectPermissions() {
    var bootstrap = actors.asAnyAdmin();
    var group = bootstrap.secretGroups().create("gov-test-" + UUID.randomUUID(), "AES-256-GCM");

    String readerName = "reader-" + UUID.randomUUID();
    String pass = "Password123!";
    var readerUser = bootstrap.users().create(readerName, pass);
    var reader = actors.asUser(readerName, pass);

    // Bootstrap creates a secret
    bootstrap.secrets().create(group.getId(), "admin-secret", "highly-sensitive");

    // 1. Reader has NO access yet
    reader.secrets().listRaw(group.getId(), Map.of()).then().statusCode(403);

    // 2. Grant READ only to Reader
    bootstrap
        .authorizations()
        .update(group.getId(), readerUser.getId(), EnumSet.of(PermissionType.READ));

    // 3. Verify Reader can read metadata and value
    assertThat(reader.secrets().list(group.getId(), Map.of()).getItems()).isNotEmpty();
    assertThat(reader.secrets().getValue(group.getId(), "admin-secret").getPlaintextValue())
        .isEqualTo("highly-sensitive");

    // 4. Verify Reader CANNOT create, update or delete
    reader
        .secrets()
        .createRaw(group.getId(), new CreateSecretRequest("fail", "val"))
        .then()
        .statusCode(403);
    reader
        .secrets()
        .updateValueRaw(group.getId(), "admin-secret", new UpdateSecretValueRequest("hack"))
        .then()
        .statusCode(403);
    reader.secrets().deleteRaw(group.getId(), "admin-secret").then().statusCode(403);
  }
}
