package com.example.secrets_manager.core.models;

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
  private String accessToken;
  private Instant accessTokenExpiresAt;
  private String refreshToken;
  private Instant refreshTokenExpiresAt;
}
