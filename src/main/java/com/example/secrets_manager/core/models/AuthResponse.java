package com.example.secrets_manager.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("access_token_expires_at")
  private Instant accessTokenExpiresAt;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("refresh_token_expires_at")
  private Instant refreshTokenExpiresAt;
}
