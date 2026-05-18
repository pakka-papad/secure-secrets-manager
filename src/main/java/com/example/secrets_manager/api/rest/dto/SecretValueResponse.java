package com.example.secrets_manager.api.rest.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecretValueResponse {
  private String name;
  @ToString.Exclude private String plaintextValue;
}
