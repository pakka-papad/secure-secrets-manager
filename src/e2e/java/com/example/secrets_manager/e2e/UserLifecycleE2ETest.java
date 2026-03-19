package com.example.secrets_manager.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.core.models.UserRole;
import org.junit.jupiter.api.Test;

class UserLifecycleE2ETest extends E2EBaseTest {

  @Test
  void shouldPerformFullUserLifecycle() {
    String username = "e2eUser" + System.currentTimeMillis();
    String password = "Password1234";

    // 1. Create a user as Admin
    var admin = actors.asBootstrapAdmin();
    admin.users().create(username, password);

    // 2. Login as the new user and verify profile
    var user = actors.asUser(username, password);
    var profile = user.users().me();

    assertThat(profile.getName()).isEqualTo(username);
    assertThat(profile.getRoles()).contains(UserRole.USER);
  }
}
