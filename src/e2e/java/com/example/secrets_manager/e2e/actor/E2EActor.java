package com.example.secrets_manager.e2e.actor;

import com.example.secrets_manager.e2e.client.AdminTestClient;
import com.example.secrets_manager.e2e.client.AuditClient;
import com.example.secrets_manager.e2e.client.SecretClient;
import com.example.secrets_manager.e2e.client.SecretGroupAuthorizationClient;
import com.example.secrets_manager.e2e.client.SecretGroupClient;
import com.example.secrets_manager.e2e.client.SecurityEventClient;
import com.example.secrets_manager.e2e.client.SystemMetadataClient;
import com.example.secrets_manager.e2e.client.TaskClient;
import com.example.secrets_manager.e2e.client.UserClient;

public class E2EActor {
  private final String token;
  private final String username;
  private final String password;
  private UserClient userClient;
  private SecretGroupClient secretGroupClient;
  private SecretClient secretClient;
  private SecretGroupAuthorizationClient secretGroupAuthorizationClient;
  private SystemMetadataClient systemMetadataClient;
  private TaskClient taskClient;
  private AdminTestClient adminTestClient;
  private AuditClient auditClient;
  private SecurityEventClient securityEventClient;

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

  public SecretClient secrets() {
    if (secretClient == null) {
      secretClient = new SecretClient(token);
    }
    return secretClient;
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

  public TaskClient tasks() {
    if (taskClient == null) {
      taskClient = new TaskClient(token);
    }
    return taskClient;
  }

  public AuditClient audit() {
    if (auditClient == null) {
      auditClient = new AuditClient(token);
    }
    return auditClient;
  }

  public SecurityEventClient securityEvents() {
    if (securityEventClient == null) {
      securityEventClient = new SecurityEventClient(token);
    }
    return securityEventClient;
  }

  public AdminTestClient test() {
    if (adminTestClient == null) {
      adminTestClient = new AdminTestClient(token);
    }
    return adminTestClient;
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
