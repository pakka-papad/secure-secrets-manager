package com.example.secrets_manager.api.rest.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response representing a secret group's metadata. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretGroupResponse {
  private UUID id;
  private String name;
  private String encryptAlgo;
  private Instant createdAt;
  private Instant modifiedAt;
}
