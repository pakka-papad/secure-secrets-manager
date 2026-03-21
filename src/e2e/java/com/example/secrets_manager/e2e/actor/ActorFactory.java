package com.example.secrets_manager.e2e.actor;

import com.example.secrets_manager.core.models.UserRole;
import com.example.secrets_manager.e2e.client.AuthClient;

public class ActorFactory {
  private final AuthClient authClient = new AuthClient();

  public ActorFactory(String adminUser, String adminPass) {
    synchronized (ActorFactory.class) {
      if (!ActorRegistry.isEmpty()) {
        return;
      }
      final var initUser = asUser(adminUser, adminPass);
      final var me = initUser.users().me();
      ActorRegistry.register(new UserCredential(me.getId(), adminUser, adminPass, me.getRoles()));
    }
  }

  public E2EActor asAnyAdmin() {
    return asAny(UserRole.ADMIN);
  }

  public E2EActor asAnyUser() {
    return asAny(UserRole.USER);
  }

  public E2EActor asAnySecretManager() {
    return asAny(UserRole.SECRET_MANAGER);
  }

  public E2EActor asAny(UserRole role) {
    UserCredential credential = ActorRegistry.findAny(role);
    return asUser(credential.username(), credential.password());
  }

  public E2EActor asUser(String username, String password) {
    String token = authClient.login(username, password);
    return new E2EActor(token, username, password);
  }
}
