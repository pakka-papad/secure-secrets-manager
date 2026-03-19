package com.example.secrets_manager.e2e.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.secrets_manager.e2e.actor.E2EActor;
import com.example.secrets_manager.e2e.base.E2EBaseTest;
import com.example.secrets_manager.e2e.client.AuthClient;
import org.junit.jupiter.api.Test;

class AuthRefreshE2ETest extends E2EBaseTest {

  @Test
  void shouldRotateTokensDuringRefresh() {
    var authClient = new AuthClient();

    // 1. Initial Login
    var initialResponse =
        authClient.loginExtended(BOOTSTRAP_ADMIN_USERNAME, BOOTSTRAP_ADMIN_PASSWORD);
    String firstAccessToken = initialResponse.getAccessToken();
    String firstRefreshToken = initialResponse.getRefreshToken();
    assertThat(firstAccessToken).isNotNull();
    assertThat(firstRefreshToken).isNotNull();

    // 2. Perform Refresh
    var refreshResponse = authClient.refresh(firstRefreshToken);

    // 3. Verify new tokens are different (Rotation)
    assertThat(refreshResponse.getAccessToken()).isNotEqualTo(firstAccessToken);
    assertThat(refreshResponse.getRefreshToken()).isNotEqualTo(firstRefreshToken);

    // 4. Verify new Access Token works
    var actor = new E2EActor(refreshResponse.getAccessToken());
    var profile = actor.users().me();
    assertThat(profile.getName()).isEqualTo(BOOTSTRAP_ADMIN_USERNAME);
  }
}
