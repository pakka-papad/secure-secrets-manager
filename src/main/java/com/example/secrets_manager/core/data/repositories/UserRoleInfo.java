package com.example.secrets_manager.core.data.repositories;

import java.util.UUID;

/** Surgical projection for user identity and roles. */
public interface UserRoleInfo {
  UUID getId();

  String getName();

  String[] getRoles();
}
