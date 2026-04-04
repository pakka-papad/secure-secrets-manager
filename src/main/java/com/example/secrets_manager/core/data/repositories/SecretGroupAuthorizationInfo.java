package com.example.secrets_manager.core.data.repositories;

import java.time.Instant;
import java.util.UUID;

public interface SecretGroupAuthorizationInfo {
  UUID getUserId();

  String getUsername();

  boolean isPRead();

  boolean isPWrite();

  boolean isPDelete();

  Instant getModifiedAt();
}
