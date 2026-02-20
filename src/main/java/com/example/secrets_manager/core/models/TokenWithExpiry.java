package com.example.secrets_manager.core.models;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Represents a generated JWT along with its absolute expiration timestamp. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenWithExpiry {
  private String token;
  private Instant expiry;
}
