package com.example.secrets_manager.e2e.actor;

import com.example.secrets_manager.e2e.client.AuthClient;

public class ActorFactory {
  private final AuthClient authClient = new AuthClient();
  private final String adminUser;
  private final String adminPass;

  public ActorFactory(String adminUser, String adminPass) {
    this.adminUser = adminUser;
    this.adminPass = adminPass;
  }

  public E2EActor asBootstrapAdmin() {
    return asUser(adminUser, adminPass);
  }

  public E2EActor asUser(String username, String password) {
    String token = authClient.login(username, password);
    return new E2EActor(token);
  }
}
