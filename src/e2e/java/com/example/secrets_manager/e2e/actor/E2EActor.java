package com.example.secrets_manager.e2e.actor;

import com.example.secrets_manager.e2e.client.SecretGroupClient;
import com.example.secrets_manager.e2e.client.SystemMetadataClient;
import com.example.secrets_manager.e2e.client.UserClient;

public class E2EActor {
  private final String token;
  private UserClient userClient;
  private SecretGroupClient secretGroupClient;
  private SystemMetadataClient systemMetadataClient;

  public E2EActor(String token) {
    this.token = token;
  }

  public UserClient users() {
    if (userClient == null) {
      userClient = new UserClient(token);
    }
    return userClient;
  }

  public SecretGroupClient secretGroups() {
    if (secretGroupClient == null) {
      secretGroupClient = new SecretGroupClient(token);
    }
    return secretGroupClient;
  }

  public SystemMetadataClient systemMetadata() {
    if (systemMetadataClient == null) {
      systemMetadataClient = new SystemMetadataClient(token);
    }
    return systemMetadataClient;
  }

  public String getToken() {
    return token;
  }
}
