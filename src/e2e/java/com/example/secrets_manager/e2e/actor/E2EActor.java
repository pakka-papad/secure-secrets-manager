package com.example.secrets_manager.e2e.actor;

import com.example.secrets_manager.e2e.client.SecretGroupAuthorizationClient;
import com.example.secrets_manager.e2e.client.SecretGroupClient;
import com.example.secrets_manager.e2e.client.SystemMetadataClient;
import com.example.secrets_manager.e2e.client.UserClient;

public class E2EActor {
  private final String token;
  private final String username;
  private final String password;
  private UserClient userClient;
  private SecretGroupClient secretGroupClient;
  private SecretGroupAuthorizationClient secretGroupAuthorizationClient;
  private SystemMetadataClient systemMetadataClient;

  public E2EActor(String token, String username, String password) {
    this.token = token;
    this.username = username;
    this.password = password;
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

  public SecretGroupAuthorizationClient authorizations() {
    if (secretGroupAuthorizationClient == null) {
      secretGroupAuthorizationClient = new SecretGroupAuthorizationClient(token);
    }
    return secretGroupAuthorizationClient;
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

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
