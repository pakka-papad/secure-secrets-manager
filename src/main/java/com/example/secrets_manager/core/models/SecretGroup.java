package com.example.secrets_manager.core.models;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretGroup {
  private UUID id;
  private String name;
  private String encryptAlgo;
  private Instant createdAt;
  private Instant modifiedAt;
  private Instant deletedAt;
}
