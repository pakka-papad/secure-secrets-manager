package com.example.secrets_manager.e2e.actor;

import com.example.secrets_manager.e2e.client.UserClient;

public class E2EActor {
  private final String token;
  private UserClient userClient;

  public E2EActor(String token) {
    this.token = token;
  }

  public UserClient users() {
    if (userClient == null) {
      userClient = new UserClient(token);
    }
    return userClient;
  }

  public String getToken() {
    return token;
  }
}
