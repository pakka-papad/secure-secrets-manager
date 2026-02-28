package com.example.secrets_manager.api.rest.dto;

import com.example.secrets_manager.core.models.UserRole;
import java.util.EnumSet;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreationRequest {
  private String name;
  private String password;
  private EnumSet<UserRole> roles;
}
