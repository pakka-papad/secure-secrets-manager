package com.example.secrets_manager.core.models;

import com.example.secrets_manager.core.validators.EncryptionAlgorithm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload for creating a new secret group. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretGroupCreationPayload {
  @NotBlank
  @Size(max = 255)
  private String name;

  @NotBlank
  @Size(max = 31)
  @EncryptionAlgorithm
  private String encryptAlgo;
}
