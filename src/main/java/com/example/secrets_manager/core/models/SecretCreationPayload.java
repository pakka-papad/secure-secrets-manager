package com.example.secrets_manager.core.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretCreationPayload {
  @NotBlank
  @Size(max = 511)
  private String name;

  @NotNull private String plaintextValue;
}
