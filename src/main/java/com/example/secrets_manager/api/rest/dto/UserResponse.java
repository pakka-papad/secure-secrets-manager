package com.example.secrets_manager.api.rest.dto;

import com.example.secrets_manager.core.models.UserRole;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
  private UUID id;
  private String name;
  private EnumSet<UserRole> roles;
  private Instant createdAt;
  private Instant modifiedAt;
}
