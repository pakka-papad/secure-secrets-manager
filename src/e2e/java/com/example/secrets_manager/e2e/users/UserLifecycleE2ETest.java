package com.example.secrets_manager.e2e.users;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UserLifecycleE2ETest extends E2EBaseTest {

  @Test
  void shouldPerformFullUserLifecycle() {
    String username = "e2eUser" + System.currentTimeMillis();
    String password = "Password1234";

    // 1. Create a user as Admin
    var admin = actors.asAnyAdmin();
    admin.users().create(username, password);

    // 2. Login as the new user and verify profile
    var user = actors.asUser(username, password);
    var profile = user.users().me();

    assertThat(profile.getName()).isEqualTo(username);
    assertThat(profile.getRoles()).contains(UserRole.USER);
  }

  @Test
  void userShouldBeAbleToSeeOtherRolesInList() {
    String smName = "smUser" + System.currentTimeMillis();
    String userName = "regUser" + System.currentTimeMillis();
    String password = "Password1234";

    // 1. Create a Secret Manager and a regular User as Admin
    var admin = actors.asAnyAdmin();
    admin.users().create(smName, password, Set.of("SECRET_MANAGER"));
    admin.users().create(userName, password, Set.of("USER"));

    // 2. Login as the regular user
    var user = actors.asUser(userName, password);

    // 3. List all users and verify visibility of Admin and Secret Manager
    var allUsers = user.users().list(Map.of("size", "200")).getItems();

    assertThat(allUsers).anyMatch(u -> u.getRoles().contains(UserRole.ADMIN));
    assertThat(allUsers).anyMatch(u -> u.getRoles().contains(UserRole.SECRET_MANAGER));
    assertThat(allUsers).anyMatch(u -> u.getName().equals(userName));
  }
}
