package com.example.secrets_manager.core.models;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for the token refresh request. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenPayload {
  @NotBlank(message = "Refresh token is required")
  private String refreshToken;
}
