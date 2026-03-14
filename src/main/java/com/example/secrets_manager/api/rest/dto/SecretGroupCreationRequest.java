package com.example.secrets_manager.api.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request payload for creating a new secret group. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretGroupCreationRequest {
  @NotBlank
  @Size(max = 255)
  private String name;

  @NotBlank
  @Size(max = 31)
  private String encryptAlgo;
}
