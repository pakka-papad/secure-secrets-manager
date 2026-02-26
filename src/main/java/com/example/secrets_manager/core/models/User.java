package com.example.secrets_manager.core.models;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
  private UUID id;
  private String name;
  @ToString.Exclude private byte[] pwSalt;
  @ToString.Exclude private byte[] pwDigest;
  private Instant createdAt;
  private Instant modifiedAt;
  private String hashAlgo;
  private String hashParams;
  private EnumSet<UserRole> roles;
  private Instant deletedAt;
}
