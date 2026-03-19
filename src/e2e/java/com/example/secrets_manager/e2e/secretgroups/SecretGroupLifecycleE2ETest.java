package com.example.secrets_manager.e2e.secretgroups;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.e2e.base.E2EBaseTest;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SecretGroupLifecycleE2ETest extends E2EBaseTest {

  @Test
  void shouldManageSecretGroupLifecycle() {
    String managerName = "manager" + System.currentTimeMillis();
    String groupName = "group" + System.currentTimeMillis();

    var admin = actors.asBootstrapAdmin();

    // 1. Create a Secret Manager
    admin.users().create(managerName, "Pass1234", Set.of("SECRET_MANAGER"));
    var manager = actors.asUser(managerName, "Pass1234");

    // 2. Manager creates a group
    var group = manager.secretGroups().create(groupName, "AES-256-GCM");
    assertThat(group.getName()).isEqualTo(groupName);

    // 3. Verify group is in manager's list
    var managerGroups = manager.secretGroups().list(Map.of());
    assertThat(managerGroups.getItems()).anyMatch(g -> g.getId().equals(group.getId()));

    // 4. Create a second user and verify they CANNOT see the group
    String otherName = "other" + System.currentTimeMillis();
    admin.users().create(otherName, "Pass1234", Set.of("USER"));
    var other = actors.asUser(otherName, "Pass1234");

    var otherGroups = other.secretGroups().list(Map.of());
    assertThat(otherGroups.getItems()).noneMatch(g -> g.getId().equals(group.getId()));

    // 5. Manager deletes the group
    manager.secretGroups().delete(group.getId());

    // 6. Verify it's gone
    var finalGroups = manager.secretGroups().list(Map.of());
    assertThat(finalGroups.getItems()).noneMatch(g -> g.getId().equals(group.getId()));
  }
}
