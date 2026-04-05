package com.example.secrets_manager.e2e.secretgroups;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.models.PermissionType;
import com.example.secrets_manager.e2e.actor.E2EActor;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SecretGroupAuthorizationE2ETest extends E2EBaseTest {

  private static final String DEFAULT_PASSWORD = "Password1234!";

  @Test
  @DisplayName("Governance: Mirroring principle should prevent privilege escalation")
  void mirroringPrinciple_ShouldPreventPrivilegeEscalation() {
    final var groupName = "secret-group-" + UUID.randomUUID();
    var setup = setupGroupWithManagers(groupName);

    // Manager A grants READ, WRITE to Manager B
    setup
        .managerA
        .authorizations()
        .update(
            setup.groupId, setup.userBId, EnumSet.of(PermissionType.READ, PermissionType.WRITE));

    // Manager B verifies access
    var bGroup = setup.managerB.secretGroups().get(setup.groupId);
    assertThat(bGroup.getName()).isEqualTo(groupName);

    // Manager B attempts to grant DELETE to themselves (Should fail - Mirroring Principle)
    setup
        .managerB
        .authorizations()
        .updateRaw(
            setup.groupId,
            setup.userBId,
            EnumSet.of(PermissionType.READ, PermissionType.WRITE, PermissionType.DELETE))
        .then()
        .statusCode(403);
  }

  @Test
  @DisplayName("Governance: Only Managers can hold DELETE permission")
  void governanceGuardrails_ShouldRestrictStandardUsers() {
    var setup = setupGroupWithManagers("secret-group-" + UUID.randomUUID());
    var bootstrap = actors.asAnyAdmin();

    String userName = "std-user-" + UUID.randomUUID();
    var stdUser = bootstrap.users().create(userName, DEFAULT_PASSWORD, Set.of("USER"));

    // Admin attempts to grant DELETE to standard USER (Should fail - Governance Block)
    bootstrap
        .authorizations()
        .updateRaw(
            setup.groupId, stdUser.getId(), EnumSet.of(PermissionType.READ, PermissionType.DELETE))
        .then()
        .statusCode(403);
  }

  @Test
  @DisplayName("Authorization: Administrators should have virtual full access")
  void effectivePermissions_ShouldAllowAdminBypass() {
    var setup = setupGroupWithManagers("secret-group-" + UUID.randomUUID());
    var bootstrap = actors.asAnyAdmin();

    // Global Admin (who is NOT in ACL) lookups their own effective permissions
    var adminAuth = bootstrap.authorizations().get(setup.groupId, bootstrap.users().me().getId());
    assertThat(adminAuth.getPermissions()).containsAll(EnumSet.allOf(PermissionType.class));
    assertThat(adminAuth.getModifiedAt()).isNull(); // Derived virtual access

    // Listing should NOT show the admin
    var aclList = setup.managerA.authorizations().list(setup.groupId, Map.of());
    assertThat(aclList.getItems())
        .noneMatch(auth -> auth.getUsername().equals(bootstrap.getUsername()));
  }

  @Test
  @DisplayName("Authorization: Revoking access should be surgical and effective")
  void revocation_ShouldRemoveAccessSurgically() {
    var setup = setupGroupWithManagers("secret-group-" + UUID.randomUUID());

    // Grant access
    setup
        .managerA
        .authorizations()
        .update(setup.groupId, setup.userBId, EnumSet.of(PermissionType.READ));

    // Verify access works
    setup.managerB.secretGroups().get(setup.groupId);

    // Revoke access
    setup.managerA.authorizations().revoke(setup.groupId, setup.userBId);

    // Manager B attempts access (Should fail)
    setup.managerB.authorizations().getRaw(setup.groupId).then().statusCode(403);
  }

  @Test
  @DisplayName("Lifecycle: Authorizations for deleted users should fail")
  void lifecycleProtection_ShouldHandleDeletedUsers() {
    var setup = setupGroupWithManagers("secret-group-" + UUID.randomUUID());
    var bootstrap = actors.asAnyAdmin();

    String deleteMeName = "delete-me-" + UUID.randomUUID();
    var toDelete = bootstrap.users().create(deleteMeName, DEFAULT_PASSWORD);
    bootstrap.users().delete(toDelete.getId());

    // Attempt to grant access to deleted user (Should fail)
    bootstrap
        .authorizations()
        .updateRaw(setup.groupId, toDelete.getId(), EnumSet.of(PermissionType.READ))
        .then()
        .statusCode(404);
  }

  private GroupSetup setupGroupWithManagers(final String groupName) {
    var anyAdmin = actors.asAnyAdmin();

    String managerAName = "manager-a-" + UUID.randomUUID();
    String managerBName = "manager-b-" + UUID.randomUUID();

    var userA = anyAdmin.users().create(managerAName, DEFAULT_PASSWORD, Set.of("SECRET_MANAGER"));
    var userB = anyAdmin.users().create(managerBName, DEFAULT_PASSWORD, Set.of("SECRET_MANAGER"));

    var managerA = actors.asUser(managerAName, DEFAULT_PASSWORD);
    var managerB = actors.asUser(managerBName, DEFAULT_PASSWORD);

    var group = managerA.secretGroups().create(groupName, "AES-256-GCM");

    return new GroupSetup(managerA, managerB, group.getId(), userA.getId(), userB.getId());
  }

  private record GroupSetup(
      E2EActor managerA, E2EActor managerB, UUID groupId, UUID userAId, UUID userBId) {}
}
